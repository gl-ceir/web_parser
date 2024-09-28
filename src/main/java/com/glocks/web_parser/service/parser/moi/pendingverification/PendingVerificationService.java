package com.glocks.web_parser.service.parser.moi.pendingverification;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class PendingVerificationService {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final AppConfig appConfig;
    private final MDRRepository mdrRepository;
    private final MOIService moiService;
    private final LostDeviceMgmtRepository lostDeviceMgmtRepository;
    private final LostDeviceDetailRepository lostDeviceDetailRepository;
    private final EirsInvalidImeiRepository eirsInvalidImeiRepository;
    private final DuplicateDeviceDetailRepository duplicateDeviceDetailRepository;
    private final WebActionDbRepository webActionDbRepository;
    private final NotificationForPendingVerification notificationForPendingVerification;
    static String runningImei = null;
    static int successCount, failCount, i = 0;

    public void pendingVerificationFileValidation(WebActionDb webActionDb, String uploadedFilePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String uploadedFileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(uploadedFilePath))) {
            String record;
            String[] header;
            boolean headerSkipped = false;
            while ((record = reader.readLine()) != null) {
                try {
                    if (!record.trim().isEmpty()) {
                        if (!headerSkipped) {
                            header = record.split(appConfig.getListMgmtFileSeparator(), -1);
                            headerSkipped = true;
                            printWriter.println(moiService.joiner(header, ""));
                        } else {
                            processRecord(record, printWriter);
                        }
                    }
                } catch (Exception e) {
                    logger.info("Exception occured inside while block for {} {}", runningImei, e.getMessage());
                }
            }
        } catch (Exception ex) {
            logger.error("Exception in processing the file {}", ex.getMessage());
        }
        printWriter.close();

        if (failCount == 0) {
            logger.info("GMSA Compliant");


        } else {
            logger.info("GMSA Non-Compliant");
            //  String channel = lostDeviceMgmt.getDeviceOwnerNationality() == null ? "SMS" : lostDeviceMgmt.getDeviceOwnerNationality();
            BiConsumer<String, String> updateStatusInLostDeviceMgmt = moiService::updateStatusInLostDeviceMgmt;
            updateStatusInLostDeviceMgmt.accept("FAIL", lostDeviceMgmt.getRequestId());
            webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
            notificationForPendingVerification.sendNotification(webActionDb, lostDeviceMgmt, "SMS", uploadedFileName, uploadedFilePath);
        }
    }

    public Boolean task2(String key, String value) {
        List<String> listStatus = List.of("INIT", "VERIFY_MOI");
        Boolean isExist = false;
        switch (key) {
            case "imei1" -> isExist = lostDeviceMgmtRepository.existsByImei1AndStatusIn(value, listStatus);
            case "imei2" -> isExist = lostDeviceMgmtRepository.existsByImei2AndStatusIn(value, listStatus);
            case "imei3" -> isExist = lostDeviceMgmtRepository.existsByImei3AndStatusIn(value, listStatus);
            case "imei4" -> isExist = lostDeviceMgmtRepository.existsByImei4AndStatusIn(value, listStatus);
        }
        return isExist;
    }

    private void processRecord(String record, PrintWriter printWriter) {
        String[] split = record.split(appConfig.getListMgmtFileSeparator(), -1);
        IMEISeriesModel imeiSeriesModel = new IMEISeriesModel(split);
        logger.info("IMEISeriesModel {}", imeiSeriesModel);

        for (Map.Entry<String, String> entry : imeiSeriesModel.toMap().entrySet()) {
            String imei = entry.getKey();
            String imeiValue = entry.getValue();
            logger.info("Key: {}, Value: {}", imei, imeiValue);

            if (!processImei(imei, imeiValue, split, printWriter)) {
                break;
            }
        }
        logger.info("row {} -- failCount {}", ++i, failCount);
    }

    private boolean processImei(String imei, String imeiValue, String[] split, PrintWriter printWriter) {
        if (!isImeiValid(imeiValue, split, printWriter)) return false;

        String tacFromIMEI = moiService.getTacFromIMEI(imeiValue);
        if (!isGSMACompliant(tacFromIMEI, split, printWriter)) return false;

        logger.info("IMEI {} is GSMA compliant ✓", imeiValue);
        if (isAlreadyPresentInLostStolen(imei, imeiValue, split, printWriter)) return false;

        if (lostDeviceDetailRepository.existsByImei(imeiValue)) {
            printWriter.println(moiService.joiner(split, ",Fail, Already present in lost/stolen"));
            failCount++;
            return false;
        }

        if (eirsInvalidImeiRepository.existsByImei(imeiValue)) {
            printWriter.println(moiService.joiner(split, ",Fail, IMEI is not valid"));
            failCount++;
            return false;
        }

        return checkDuplicateDevice(imeiValue, split, printWriter);
    }

    private boolean isImeiValid(String imeiValue, String[] split, PrintWriter printWriter) {
        boolean isImeiValid = moiService.isNumericAndValid.test(imeiValue);
        if (!isImeiValid) {
            logger.info("Invalid IMEI {}", imeiValue);
            printWriter.println(moiService.joiner(split, ", Fail,Invalid IMEI"));
            failCount++;
        }
        return isImeiValid;
    }

    private boolean isGSMACompliant(String tacFromIMEI, String[] split, PrintWriter printWriter) {
        if (!mdrRepository.existsByDeviceIdAndIsTypeApproved(tacFromIMEI, 1)) {
            printWriter.println(moiService.joiner(split, ", Fail,GSMA non compliant"));
            failCount++;
            return false;
        }
        return true;
    }

    private boolean checkDuplicateDevice(String imeiValue, String[] split, PrintWriter printWriter) {
        if (duplicateDeviceDetailRepository.existsByImeiAndMsisdnNull(imeiValue)) {
            logger.info("Phone number not provided for IMEI {}", imeiValue);
            printWriter.println(moiService.joiner(split, ",Fail, IMEI identified as duplicate"));
            failCount++;
            return false;
        }

        if (!duplicateDeviceDetailRepository.existsByImeiAndStatusIgnoreCaseEquals(imeiValue, "ORIGINAL")) {
            logger.info("Phone number provided for IMEI {} but status is not equals to ORIGINAL  ❌", imeiValue);
            printWriter.println(moiService.joiner(split, ",Fail, IMEI identified as duplicate"));
            failCount++;
            return false;
        }

        logger.info("Phone number provided for IMEI {} and status is ORIGINAL in duplicate_device_detail ✓", imeiValue);
        return true;
    }

    private boolean isAlreadyPresentInLostStolen(String imei, String imeiValue, String[] split, PrintWriter printWriter) {
        if (task2(imei, imeiValue)) {
            printWriter.println(moiService.joiner(split, ",Fail, Already present in lost/stolen"));
            failCount++;
            return true;
        }
        return false;
    }
}

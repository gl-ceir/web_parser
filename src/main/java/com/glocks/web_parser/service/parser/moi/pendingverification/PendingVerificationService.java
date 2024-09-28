package com.glocks.web_parser.service.parser.moi.pendingverification;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceDetail;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
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
    static int successCount, failCount, i = 0;
    List<LostDeviceDetail> lostDeviceDetailPayload = new LinkedList<>();

    public void payload(String imei, LostDeviceMgmt lostDeviceMgmt) {
        lostDeviceDetailPayload.add(LostDeviceDetail.builder()
                .imei(imei)
                .requestId(lostDeviceMgmt.getRequestId())
                .contactNumber(lostDeviceMgmt.getContactNumberForOtp())
                .deviceBrand(lostDeviceMgmt.getDeviceBrand())
                .deviceModel(lostDeviceMgmt.getDeviceModel())
                .requestType(lostDeviceMgmt.getRequestType())
                .status("PENDING_VERIFICATION")
                .build());
    }

    public boolean pendingVerificationFileValidation(WebActionDb webActionDb, String filePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String uploadedFileName, String state) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String record;
            String[] header;
            boolean headerSkipped = false;
            while ((record = reader.readLine()) != null) {
                try {
                    if (!record.trim().isEmpty()) {
                        if (!headerSkipped) {
                            header = record.split(appConfig.getListMgmtFileSeparator(), -1);
                            headerSkipped = true;
                            //printWriter.println(moiService.joiner(header, ",Reason"));
                            if (state.equalsIgnoreCase("0")) printWriter.println(moiService.joiner(header, ""));
                        } else {
                            String[] split = record.split(appConfig.getListMgmtFileSeparator(), -1);
                            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel(split);
                            logger.info("IMEISeriesModel {}", imeiSeriesModel);
                            for (Map.Entry<String, String> entry : imeiSeriesModel.toMap().entrySet()) {
                                String imei = entry.getKey();
                                String imeiValue = entry.getValue();
                                logger.info("Key: " + imei + ", Value: " + imeiValue);
                                if (state.equalsIgnoreCase("0"))
                                    if (!isConditionFulfil(imei, imeiValue, printWriter, split)) break;

                                if (state.equalsIgnoreCase("1")) {
                                    payload(imeiValue, lostDeviceMgmt);
                                }

                            }
                            if (state.equalsIgnoreCase("1")) {
                                printWriter.println(moiService.joiner(split, ",Success,Success"));
                            }
                            logger.info("row {} <<>> failCount {}", ++i, failCount);
                        }
                    }
                } catch (Exception e) {
                    logger.info("Exception occured inside while block for {}", e.getMessage());
                }
            }
        } catch (Exception ex) {
            logger.error("Exception in processing the file {}", ex.getMessage());
        }
        return failCount == 0;
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


    public void validFile(WebActionDb webActionDb, String uploadedFilePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String uploadedFileName, String state) {
        logger.info("File passed");
        this.pendingVerificationFileValidation(webActionDb, uploadedFilePath, lostDeviceMgmt, printWriter, uploadedFileName, state);
        printWriter.close();
        logger.info("lostDeviceDetail payload {}", lostDeviceDetailPayload);
        try {
            lostDeviceDetailRepository.saveAll(lostDeviceDetailPayload);
            BiConsumer<String, String> updateStatusInLostDeviceMgmt = moiService::updateStatusInLostDeviceMgmt;
            updateStatusInLostDeviceMgmt.accept("VERIFY_MOI", lostDeviceMgmt.getRequestId());
            webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        } catch (Exception e) {
            logger.info("Exception occured during save operation {}", e.getMessage());
        }
    }

    public void invalidFile(WebActionDb webActionDb, String uploadedFilePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String uploadedFileName, String state) {
        logger.info("File failed");
        //  String channel = lostDeviceMgmt.getDeviceOwnerNationality() == null ? "SMS" : lostDeviceMgmt.getDeviceOwnerNationality();
        BiConsumer<String, String> updateStatusInLostDeviceMgmt = moiService::updateStatusInLostDeviceMgmt;
        updateStatusInLostDeviceMgmt.accept("FAIL", lostDeviceMgmt.getRequestId());
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        notificationForPendingVerification.sendNotification(webActionDb, lostDeviceMgmt, "SMS", uploadedFileName, uploadedFilePath);
        printWriter.close();
    }

    public boolean isConditionFulfil(String imei, String imeiValue, PrintWriter printWriter, String[] split) {
        boolean isImeiValid = moiService.isNumericAndValid.test(imeiValue);
        if (!isImeiValid) {
            logger.info("Invalid IMEI {}", imeiValue);
            printWriter.println(moiService.joiner(split, ",Fail,Invalid IMEI"));
            ++failCount;
            return false;
        }

        String tacFromIMEI = moiService.getTacFromIMEI(imeiValue);
//      Not GSMA compliant
        if (!mdrRepository.existsByDeviceIdAndIsTypeApproved(tacFromIMEI, 1)) {
            printWriter.println(moiService.joiner(split, ",Fail,GSMA non compliant"));
            ++failCount;
            return false;
        }
//     Is IMEI present in lost_device_mgmt
        logger.info("IMEI {} is GSMA compliant ✓", imeiValue);
        if (task2(imei, imeiValue)) {
            printWriter.println(moiService.joiner(split, ",Fail,Already present in lost/stolen"));
            ++failCount;
            return false;
        }

//     Is IMEI present in lost_device_detail
        logger.info("No IMEI {} found for lost device mgmt ❌", imeiValue);
        if (lostDeviceDetailRepository.existsByImei(imeiValue)) {
            printWriter.println(moiService.joiner(split, ",Fail,Already present in lost/stolen"));
            ++failCount;
            return false;
        }
//     Is IMEI present in eirs_invalid_imei
        logger.info("No IMEI {} found for lost_device_detail ❌", imeiValue);
        if (eirsInvalidImeiRepository.existsByImei(imeiValue)) {
            printWriter.println(moiService.joiner(split, ",Fail,IMEI is not valid"));
            ++failCount;
            return false;
        }
//     Is IMEI present in duplicate_device_detail
        logger.info("No IMEI {} found for eirs_invalid_imei ❌", imeiValue);
        if (duplicateDeviceDetailRepository.existsByImeiAndMsisdnNull(imeiValue)) {
            logger.info("Phone number not provided for IMEI {}", imeiValue);
            printWriter.println(moiService.joiner(split, ",Fail,IMEI identified as duplicate"));
            ++failCount;
            return false;
        } else {
            if (!duplicateDeviceDetailRepository.existsByImeiAndStatusIgnoreCaseEquals(imeiValue, "ORIGINAL")) {
                logger.info("Phone number provided for IMEI {} but status is not equals to ORIGINAL  ❌", imeiValue);
                printWriter.println(moiService.joiner(split, ",Fail,IMEI identified as duplicate"));
                ++failCount;
                return false;
            }
            logger.info("Phone number provided for IMEI {} and status is ORIGINAL in duplicate_device_detail ✓", imeiValue);
            return true;
        }
    }
}

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
import java.util.*;
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
    static int failCount, i = 0;
    List<LostDeviceDetail> lostDeviceDetailPayload = new LinkedList<>();
    Set<String> set = new LinkedHashSet<>();

    public boolean pendingVerificationFileValidation(String filePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String state) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String record;
            String[] header;
            boolean headerSkipped = false;
            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
            String[] split;
            while ((record = reader.readLine()) != null) {
                try {
                    if (!record.trim().isEmpty()) {
                        if (!headerSkipped) {
                            header = record.split(appConfig.getListMgmtFileSeparator(), -1);
                            headerSkipped = true;
//printWriter.println(moiService.joiner(header, ",Reason"));
                            if (state.equalsIgnoreCase("0")) printWriter.println(moiService.joiner(header, ""));
                        } else {
                            split = record.split(appConfig.getListMgmtFileSeparator(), -1);
                            imeiSeriesModel.setImeiSeries(split, "STOLEN");
                            logger.info("IMEISeriesModel {}", imeiSeriesModel);
                            List<String> imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
                            if (!imeiList.isEmpty()) {
                                for (String imei : imeiList) {
                                    if (state.equalsIgnoreCase("0"))
                                        if (!isConditionFulfil(imei, printWriter, split)) break;
                                }
                            }
                            logger.info("row {} <<>> failCount {}", ++i, failCount);
                            if (state.equalsIgnoreCase("1")) {
                                printSuccessRecord(split, printWriter);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.info("Exception occured inside while block {}", e.getMessage());
                }
            }

            if (state.equalsIgnoreCase("1")) {
                LostDeviceDetail lostDeviceDetail = LostDeviceDetail.builder().requestId(lostDeviceMgmt.getRequestId()).contactNumber(lostDeviceMgmt.getContactNumberForOtp()).deviceBrand(lostDeviceMgmt.getDeviceBrand()).deviceModel(lostDeviceMgmt.getDeviceModel()).requestType(lostDeviceMgmt.getRequestType()).status("PENDING_VERIFICATION").build();
                payload(set, lostDeviceDetail);
                logger.info("lostDeviceDetail payload {}", lostDeviceDetail);
            }
        } catch (Exception ex) {
            logger.error("Exception in processing the file {}", ex.getMessage());
        }
        return failCount == 0;
    }

    public void validFile(WebActionDb webActionDb, String uploadedFilePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String uploadedFileName, String state) {
        this.pendingVerificationFileValidation(uploadedFilePath, lostDeviceMgmt, printWriter, state);
        printWriter.close();
        try {
            lostDeviceDetailRepository.saveAll(lostDeviceDetailPayload);
            logger.info("Record saved in lost_device_details for {}", lostDeviceMgmt.getRequestId());
            BiConsumer<String, String> updateStatusInLostDeviceMgmt = moiService::updateStatusInLostDeviceMgmt;
            updateStatusInLostDeviceMgmt.accept("VERIFY_MOI", lostDeviceMgmt.getRequestId());
            webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        } catch (Exception e) {
            logger.info("Exception occured during save operation {}", e.getMessage());
        }
    }

    public void invalidFile(WebActionDb webActionDb, String uploadedFilePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String uploadedFileName, String state) {
        String channel = Objects.nonNull(lostDeviceMgmt.getDeviceOwnerNationality()) ? lostDeviceMgmt.getDeviceOwnerNationality() : "EMAIL";
        BiConsumer<String, String> updateStatusInLostDeviceMgmt = moiService::updateStatusInLostDeviceMgmt;
        updateStatusInLostDeviceMgmt.accept("FAIL", lostDeviceMgmt.getRequestId());
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        notificationForPendingVerification.sendNotification(webActionDb, lostDeviceMgmt, channel, uploadedFilePath);
        printWriter.close();
    }

    public Boolean isImeiExistInLostDeviceMgmt(String value) {
        List<String> listStatus = List.of("INIT", "VERIFY_MOI");
        return lostDeviceMgmtRepository.existsByImeiAndStatusIn(value, listStatus) > 0;
    }


    public boolean isConditionFulfil(String imei, PrintWriter printWriter, String[] split) {
        boolean isImeiValid = moiService.isNumericAndValid.test(imei);
        if (!isImeiValid) {
            logger.info("Invalid IMEI {}", imei);
            printWriter.println(moiService.joiner(split, ",Fail,Invalid IMEI"));
            ++failCount;
            return false;
        }

        String tacFromIMEI = moiService.getTacFromIMEI(imei);
//      Not GSMA compliant
        if (!mdrRepository.existsByDeviceIdAndIsTypeApproved(tacFromIMEI, 1)) {
            printWriter.println(moiService.joiner(split, ",Fail,GSMA non compliant"));
            ++failCount;
            return false;
        }
//     Is IMEI present in lost_device_mgmt
        logger.info("GSMA check passed for IMEI {} ✓", imei);
        if (isImeiExistInLostDeviceMgmt(imei)) {
            printWriter.println(moiService.joiner(split, ",Fail,Already present in lost/stolen"));
            ++failCount;
            return false;
        }

//     Is IMEI present in lost_device_detail
        logger.info("No IMEI {} found in lost device mgmt ❌", imei);
        if (lostDeviceDetailRepository.existsByImei(imei)) {
            printWriter.println(moiService.joiner(split, ",Fail,Already present in lost/stolen"));
            ++failCount;
            return false;
        }
//     Is IMEI present in eirs_invalid_imei
        logger.info("No IMEI {} found in lost_device_detail ❌", imei);
        if (eirsInvalidImeiRepository.existsByImei(imei)) {
            printWriter.println(moiService.joiner(split, ",Fail,IMEI is not valid"));
            ++failCount;
            return false;
        }
//     Is IMEI present in duplicate_device_detail
        logger.info("No IMEI {} found in eirs_invalid_imei ❌", imei);
        if (duplicateDeviceDetailRepository.existsByImei(imei)) {
            logger.info("IMEI {} found in duplicate_device_detail ✓", imei);
            if (duplicateDeviceDetailRepository.existsByImeiAndMsisdnIsNull(imei)) {
                logger.info("Phone number not provided for IMEI {}", imei);
                printWriter.println(moiService.joiner(split, ",Fail,IMEI identified as duplicate"));
                ++failCount;
                return false;
            } else {
                if (!duplicateDeviceDetailRepository.existsByImeiAndStatusIgnoreCaseEquals(imei, "ORIGINAL")) {
                    logger.info("Phone number provided for IMEI {} but status is not equals to ORIGINAL  ❌", imei);
                    printWriter.println(moiService.joiner(split, ",Fail,IMEI identified as duplicate"));
                    ++failCount;
                    return false;
                }
            }
        }
        return true;
    }

    public void payload(Set<String> imeiSet, LostDeviceDetail lostDeviceDetail) {
        for (String imei : imeiSet) {
            lostDeviceDetail.setImei(imei);
            lostDeviceDetailPayload.add(lostDeviceDetail);
        }
        logger.info("lostDeviceDetail payload {}", lostDeviceDetailPayload);
    }

    public void printSuccessRecord(String[] split, PrintWriter printWriter) {
        printWriter.println(moiService.joiner(split, ",Success,Success"));
        for (String arr : split)
            set.add(arr);
    }
}

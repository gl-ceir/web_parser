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
import org.springframework.dao.DataIntegrityViolationException;
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
    static int failCount = 0;
    Set<LostDeviceDetail> set = new LinkedHashSet<>();

    public boolean pendingVerificationFileValidation(String filePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String state) {
        failCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String record;
            String[] header;
            boolean headerSkipped = false;
            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
            String[] split;
            while ((record = reader.readLine()) != null) {
                if (!record.trim().isEmpty()) {
                    if (!headerSkipped) {
                        header = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        headerSkipped = true;
                        if (state.equalsIgnoreCase("VERIFICATION_STAGE_INIT"))
                            printWriter.println(moiService.joiner(header, ",Status,Reason"));
                    } else {
                        split = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        imeiSeriesModel.setImeiSeries(split, "STOLEN");
                        List<String> imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
                        if (!imeiList.isEmpty()) {
                            action(split, printWriter, state, imeiList, lostDeviceMgmt, imeiSeriesModel);
                        }
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            logger.error("Exception in processing the file {}", exception.getMessage());
            failCount++;
        }
        logger.info("failCount {}", failCount);
        return failCount == 0;
    }


    public void validFile(WebActionDb webActionDb, String uploadedFilePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String uploadedFileName, String state) {
        this.pendingVerificationFileValidation(uploadedFilePath, lostDeviceMgmt, printWriter, state);
        printWriter.close();
        if (!set.isEmpty()) this.payload(set);
        moiService.updateStatusInLostDeviceMgmt("VERIFY_MOI", lostDeviceMgmt.getRequestId());
        logger.info("updated status as VERIFY_MOI");
        webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
        logger.info("updated state as Done against {}", webActionDb.getTxnId());

    }

    public void invalidFile(WebActionDb webActionDb, String uploadedFilePath, LostDeviceMgmt lostDeviceMgmt, PrintWriter printWriter, String uploadedFileName, String state) {
        printWriter.close();
        String channel = Objects.nonNull(lostDeviceMgmt.getDeviceOwnerNationality()) ? lostDeviceMgmt.getDeviceOwnerNationality() : "EMAIL";
        moiService.updateStatusInLostDeviceMgmt("Fail", lostDeviceMgmt.getRequestId());
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        notificationForPendingVerification.sendNotification(webActionDb, lostDeviceMgmt, channel, uploadedFilePath);
    }

    public void action(String[] split, PrintWriter printWriter, String state, List<String> imeiList, LostDeviceMgmt lostDeviceMgmt, IMEISeriesModel imeiSeriesModel) {
        boolean isImeiListValid = true;
        for (String imei : imeiList) {
            if (!moiService.isNumericAndValid(imei)) {
                logger.info("Invalid IMEI {} found", imei);
                printWriter.println(moiService.joiner(split, ",Fail,Invalid IMEI"));
            } else {
                if (state.equalsIgnoreCase("VERIFICATION_STAGE_INIT")) {
                    if (!isConditionFulfil(imei, printWriter, split)) {
                        isImeiListValid = false;
                        break;
                    }
                } else if (state.equalsIgnoreCase("VERIFICATION_STAGE_DONE")) {
                    LostDeviceDetail lostDeviceDetail = LostDeviceDetail.builder().imei(imei)
                            .requestId(lostDeviceMgmt.getRequestId())
                            .contactNumber(imeiSeriesModel.getContactNumber())
                            .deviceBrand(imeiSeriesModel.getBrand())
                            .deviceModel(imeiSeriesModel.getModel())
                            .requestType(lostDeviceMgmt.getRequestType()).status("PENDING_VERIFICATION").build();
                    set.add(lostDeviceDetail);
                }
            }
        }
        if (state.equalsIgnoreCase("VERIFICATION_STAGE_INIT")) {
            logger.info("isImeiListValid {}", isImeiListValid);
            if (isImeiListValid) printSuccessRecord(split, printWriter);
        }
    }

    public Boolean isImeiExistInLostDeviceMgmt(String value) {
        List<String> listStatus = List.of("INIT", "VERIFY_MOI");
        return lostDeviceMgmtRepository.existsByImeiAndStatusIn(value, listStatus) > 0;
    }


    public boolean isConditionFulfil(String imei, PrintWriter printWriter, String[] split) {
        boolean isImeiValid = moiService.isNumericAndValid.test(imei);
        if (!isImeiValid) {
            logger.info("Invalid IMEI length {}", imei);
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
            logger.info("IMEI {} found in lostDeviceMgmt", imei);
            return false;
        }

//     Is IMEI present in lost_device_detail
        logger.info("No IMEI {} found in lost device mgmt ❌", imei);
        if (lostDeviceDetailRepository.existsByImei(imei)) {
            printWriter.println(moiService.joiner(split, ",Fail,Already present in lost/stolen"));
            ++failCount;
            logger.info("IMEI {} found in lostDeviceDetail", imei);
            return false;
        }
//     Is IMEI present in eirs_invalid_imei
        logger.info("No IMEI {} found in lost_device_detail ❌", imei);
        if (eirsInvalidImeiRepository.existsByImei(imei)) {
            printWriter.println(moiService.joiner(split, ",Fail,IMEI is not valid"));
            logger.info("IMEI {} found in eirsInvalidImei", imei);
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

    public void payload(Set<LostDeviceDetail> lostDeviceDetailSet) {
        try {
            logger.info("lostDeviceDetail payload after successful verification{}", lostDeviceDetailSet);
            lostDeviceDetailRepository.saveAll(lostDeviceDetailSet);
        } catch (DataIntegrityViolationException e) {
            logger.info("IMEI {} already exist in lost_device_detail", e.getMessage());
        }
    }

    public void printSuccessRecord(String[] split, PrintWriter printWriter) {
        printWriter.println(moiService.joiner(split, ",Success,Success"));
    }
}

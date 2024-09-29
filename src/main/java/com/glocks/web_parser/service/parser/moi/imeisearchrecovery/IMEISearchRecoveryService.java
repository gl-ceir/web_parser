package com.glocks.web_parser.service.parser.moi.imeisearchrecovery;

import com.glocks.web_parser.model.app.LostDeviceDetail;
import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IMEISearchRecoveryService {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIService moiService;
    private final WebActionDbRepository webActionDbRepository;

    public boolean isBrandAndModelGenuine(WebActionDb webActionDb, IMEISeriesModel imeiSeriesModel, String transactionId) {
        List<String> list = moiService.tacList(imeiSeriesModel);
        if (list.isEmpty()) {
            return false;
        }
        if (!moiService.isBrandAndModelValid(list)) {
            moiService.updateStatusAndCountFoundInLost("Fail", 0, transactionId, "IMEI not belongs to same device brand and model");
            webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
            return false;
        }
        return true;
    }


    public boolean isRequestIdFound(String imei, String imeiNumber, WebActionDb webActionDb, String transactionId, String requestID, String mode, int successCount) {
        boolean isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice = moiService.copyRecordLostDeviceMgmtToSearchIMEIDetailByPolice(requestID);
        if (isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice)
            logger.info("Record saved in search_imei_detail_by_police for IMEI {} and  request_id {}", imei, requestID);
        else
            logger.info("No record found for IMEI {} in lost_device_mgmt for request_id {}", imei, requestID);

        if (mode.equalsIgnoreCase("SINGLE")) {
            moiService.updateStatusAndCountFoundInLost("Success", 1, transactionId, null);
            logger.info("updated record with status as DONE and count_found_in _lost as 1 for Txn ID {}", transactionId);
            webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        }
        return isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice;
    }

    public void isLostDeviceDetailEmpty(WebActionDb webActionDb, String transactionId) {
        logger.info("No record found for txn ID {} in lost_device_detail", transactionId);
        moiService.updateStatusAndCountFoundInLost("Success", 0, transactionId, "IMEI not found");
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());

    }

    public int actionAtRecord(IMEISeriesModel imeiSeriesModel, WebActionDb webActionDb, String transactionId, PrintWriter printWriter, String mode, String[] split) {
        int successCount = 0;
        boolean isLostDeviceDetailExist = false;
        logger.info("IMEISeriesModel {}", imeiSeriesModel);
        List<String> imeiList = moiService.imeiList(imeiSeriesModel);
        if (!imeiList.isEmpty()) {
            try {
                for (String imei : imeiList) {
                    Optional<LostDeviceDetail> LostDeviceDetailOptional = moiService.findByImeiAndStatusAndRequestType(imei);
                    if (LostDeviceDetailOptional.isPresent()) {
                        boolean isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice = this.isRequestIdFound(imei, imeiSeriesModel.getMap().get(imei), webActionDb, transactionId, LostDeviceDetailOptional.get().getRequestId(), "SINGLE", 1);
                        if (isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice) {
                            if (mode.equalsIgnoreCase("BULK")) {
                                printWriter.println(moiService.joiner(split, ",Found"));
                                successCount++;
                            }
                            isLostDeviceDetailExist = true;
                            break;
                        }
                    }
                }

                if (!isLostDeviceDetailExist) {
                    if (mode.equalsIgnoreCase("BULK")) {
                        printWriter.println(moiService.joiner(split, ",Not Found"));
                    }
                    this.isLostDeviceDetailEmpty(webActionDb, transactionId);
                }
            } catch (Exception e) {
                moiService.updateStatusAndCountFoundInLost("Fail", 0, transactionId, "Please try after some time");
                webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
                logger.info("Oops!, error occur while execution {}", e.getMessage());
            }
        }
        return successCount;
    }
}

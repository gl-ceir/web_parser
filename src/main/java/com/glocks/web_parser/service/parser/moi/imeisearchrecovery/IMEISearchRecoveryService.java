package com.glocks.web_parser.service.parser.moi.imeisearchrecovery;

import com.glocks.web_parser.model.app.*;
import com.glocks.web_parser.repository.app.StolenDeviceMgmtRepository;
import com.glocks.web_parser.repository.app.SearchImeiDetailByPoliceRepository;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class IMEISearchRecoveryService {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIService moiService;
    private final WebActionDbRepository webActionDbRepository;
    private final StolenDeviceMgmtRepository stolenDeviceMgmtRepository;
    public static Map<String, String> requestIdMap = new HashMap<>();
    private final SearchImeiDetailByPoliceRepository searchImeiDetailByPoliceRepository;

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

    public void isLostDeviceDetailEmpty(WebActionDb webActionDb, String transactionId, int count) {
        logger.info("No record found for txn ID {} in stolen_device_detail", transactionId);
        moiService.updateStatusAndCountFoundInLost("Done", count, transactionId, "IMEI not found");
        webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());

    }

    public void isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice(StolenDeviceMgmt lostDeviceMgmt, String requestId, String mode, String[] split, PrintWriter printWriter, String txnId, String imei, List<String> imeiList) {
        logger.info("requestIdMap {}", requestIdMap);
        logger.info("Inside isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice block");
        String deviceLostDateTime = lostDeviceMgmt.getDeviceLostDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime lostDateTime = LocalDateTime.parse(deviceLostDateTime, formatter);
        SearchImeiDetailByPolice searchImeiDetailByPolice = SearchImeiDetailByPolice.builder().imei(imei).lostDateTime(lostDateTime).createdBy(lostDeviceMgmt.getCreatedBy()).transactionId(txnId).requestId(lostDeviceMgmt.getRequestId()).deviceOwnerName(lostDeviceMgmt.getDeviceOwnerName()).deviceOwnerAddress(lostDeviceMgmt.getDeviceOwnerAddress()).contactNumber(lostDeviceMgmt.getContactNumber()).deviceOwnerNationalId(lostDeviceMgmt.getDeviceOwnerNationalID()).deviceLostPoliceStation(lostDeviceMgmt.getPoliceStation()).requestMode(mode).build();
        try {
            logger.info("---------- SearchImeiDetailByPolice payload ---------- {}", searchImeiDetailByPolice);
            searchImeiDetailByPoliceRepository.save(searchImeiDetailByPolice);
            logger.info("Record saved in search_imei_detail_by_police");
            if (mode.equalsIgnoreCase("BULK")) {
                logger.info("---- BULK REQUEST ----");
                if (requestIdMap.get(requestId) == null) {
                    requestIdMap.put(requestId, lostDeviceMgmt.getRequestId());
                    printWriter.println(moiService.joiner(split, ",Found"));
                }
            }
        } catch (Exception exception) {
            logger.info("exception occurred {}", exception.getMessage());
        }
    }

    public int actionAtRecord(IMEISeriesModel imeiSeriesModel, WebActionDb webActionDb, String transactionId, PrintWriter printWriter, String mode, String[] split) {
        int successCountInIMEISearchRecoveryService = 0;
        logger.info("IMEISeriesModel {}", imeiSeriesModel);
        List<String> imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
        if (!imeiList.isEmpty()) {
            try {
                for (String imei : imeiList) {
                    Optional<StolenDeviceDetail> lostDeviceDetailOptional = moiService.findByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(imei);
                    if (lostDeviceDetailOptional.isPresent()) {
                        StolenDeviceDetail lostDeviceDetail = lostDeviceDetailOptional.get();
                        logger.info("LostDeviceDetail response based {} on IMEI  {}", imei, lostDeviceDetail);
                        String requestId = lostDeviceDetail.getRequestId();
                        Optional<StolenDeviceMgmt> byRequestId = stolenDeviceMgmtRepository.findByRequestId(requestId);
                        if (byRequestId.isPresent()) {
                            ++successCountInIMEISearchRecoveryService;
                            StolenDeviceMgmt lostDeviceMgmt = byRequestId.get();
                            logger.info("lostDeviceMgmt response {}", lostDeviceMgmt);
                            isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice(lostDeviceMgmt, requestId, mode, split, printWriter, transactionId, imei, imeiList);
                        }
                    } else {
                        logger.info("No record found in stolen_device_detail for IMEI {}", imei);
                        if (mode.equalsIgnoreCase("BULK")) {
                            printWriter.println(moiService.joiner(split, ",Not Found"));
                            break;
                        }
                    }
                }
                updateStatus(mode, transactionId, successCountInIMEISearchRecoveryService, split, printWriter);
            } catch (Exception e) {
                moiService.updateStatusAndCountFoundInLost("Fail", 0, transactionId, "Please try after some time");
                webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
                logger.info("Oops!, error occur while execution {}", e.getMessage());
            }
        }
        logger.info("successCountInIMEISearchRecoveryService {}", successCountInIMEISearchRecoveryService);
        return successCountInIMEISearchRecoveryService;
    }

    public void updateStatus(String mode, String transactionId, int count, String[] split, PrintWriter printWriter) {
        switch (mode) {
            case "Single" -> {
                if (count == 0) {
                    moiService.updateReasonAndCountInSearchImeiByPoliceMgmt("Fail", "No IMEI found", transactionId, 0);
                } else if (count > 0) {
                    moiService.updateCountFoundInLost("Done", count, transactionId, null);
                    logger.info("updated record with count_found_in _lost as {} for Txn ID {}", count, transactionId);
                }
            }
        }
    }
}

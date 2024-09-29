package com.glocks.web_parser.service.parser.moi.imeisearchrecovery;

import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceDetail;
import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class IMEISearchRecoveryBulkRequest implements RequestTypeHandler<SearchImeiByPoliceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIService moiService;
    private final WebActionDbRepository webActionDbRepository;
    private final AppConfig appConfig;
    private final FileOperations fileOperations;
    private final AlertService alertService;
    private final IMEISearchRecoveryService imeiSearchRecoveryService;
    public static boolean isLostDeviceDetailExistInBulk, isRecordExistInLostDeviceDetail;
    static int successCount = 0;
    Map<String, String> map = new HashMap<>();

    @Override
    public void executeInitProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        executeValidateProcess(webActionDb, searchImeiByPoliceMgmt);
    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        String uploadedFileName = searchImeiByPoliceMgmt.getFileName();
        String transactionId = searchImeiByPoliceMgmt.getTransactionId();
        String moiFilePath = appConfig.getMoiFilePath();
        String uploadedFilePath = moiFilePath + "/" + transactionId + "/" + uploadedFileName;
        logger.info("Uploaded file path is {}", uploadedFilePath);
        if (!fileOperations.checkFileExists(uploadedFilePath)) {
            logger.error("Uploaded file does not exists in path {} for transactionId {}", uploadedFilePath, transactionId);
            alertService.raiseAnAlert(transactionId, ConfigurableParameter.ALERT_IMEI_SEARCH_RECOVERY.getValue(), "MOI Recover", uploadedFileName + " with transaction id " + transactionId, 0);
            return;
        }
        map.put("uploadedFileName", uploadedFileName);
        map.put("transactionId", transactionId);
        map.put("uploadedFilePath", uploadedFilePath);
        map.put("moiFilePath", moiFilePath);
        executeProcess(webActionDb, searchImeiByPoliceMgmt);

    }

    @Override
    public void executeProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        successCount = 0;
        String transactionId = map.get("transactionId");
        String moiPath = map.get("moiFilePath");
        String uploadedFilePath = map.get("uploadedFilePath");
        String processedFileName = transactionId + ".csv";
        String processedFilePath = moiPath + "/" + transactionId + "/" + processedFileName;
        logger.info("Processed file path is {}", processedFilePath);
        // create a file
        PrintWriter printWriter = moiService.file(processedFilePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(uploadedFilePath))) {
            String record;
            String[] header;
            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
            String[] split;
            boolean headerSkipped = false;
            while ((record = reader.readLine()) != null) {
                if (!record.trim().isEmpty()) {
                    if (!headerSkipped) {
                        header = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        //printWriter.println(moiService.joiner(header, ",Reason"));
                        printWriter.println(moiService.joiner(header, ""));
                        headerSkipped = true;
                    } else {
                        split = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        imeiSeriesModel.setImeiSeries(split);
                        logger.info("IMEISeriesModel {}", imeiSeriesModel);

                        boolean isImeiValid = Stream.of(split).allMatch(imei -> moiService.isNumericAndValid(imei));
                        if (!isImeiValid) {
                            printWriter.println(moiService.joiner(split, ",Invalid Format"));
                        } else {
                            boolean multipleIMEIExist = moiService.isMultipleIMEIExist(imeiSeriesModel);
                            if (multipleIMEIExist) {
                                if (!imeiSearchRecoveryService.isBrandAndModelGenuine(webActionDb, imeiSeriesModel, transactionId)) {
                                    printWriter.println(moiService.joiner(split, ", IMEI is not belongs to same device brand and model"));
                                    continue;
                                }
                            }
                            int count = imeiSearchRecoveryService.actionAtRecord(imeiSeriesModel, webActionDb, transactionId, printWriter, "BULK", split);
                            successCount += count;
/*                            List<String> imeiList = moiService.imeiList(imeiSeriesModel);
                            boolean isLostDeviceDetailExist = false;
                            if (!imeiList.isEmpty()) {
                                try {
                                    for (String imei : imeiList) {
                                        Optional<LostDeviceDetail> LostDeviceDetailOptional = moiService.findByImeiAndStatusAndRequestType(imei);
                                        if (LostDeviceDetailOptional.isPresent()) {
                                            boolean isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice = imeiSearchRecoveryService.isRequestIdFound(imei, imeiSeriesModel.getMap().get(imei), webActionDb, transactionId, LostDeviceDetailOptional.get().getRequestId(), "BULK", 0);
                                            if (isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice) {
                                                printWriter.println(moiService.joiner(split, ",Found"));
                                                successCount++;
                                                isLostDeviceDetailExist = true;
                                                break;
                                            }
                                        }
                                    }

                                    if (!isLostDeviceDetailExist) {
                                        imeiSearchRecoveryService.isLostDeviceDetailEmpty(webActionDb, transactionId);
                                        printWriter.println(moiService.joiner(split, ",Not Found"));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    moiService.updateStatusAndCountFoundInLost("Fail", 0, transactionId, "Please try after some time");
                                    //      webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
                                    logger.info("Oops!, error occur while execution {}", e.getMessage());
                                }
                            }*/
                        }
                    }
                }
            }
            logger.info("successCount {}", successCount);
            printWriter.close();
            moiService.updateStatusAndCountFoundInLost("DONE", successCount, transactionId, null);
            logger.info("updated record with status as DONE and count_found_in _lost as 1 for Txn ID {}", transactionId);
            // webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        } catch (Exception ex) {
            logger.error("Exception in processing the file " + ex.getMessage());
        }

    }
}

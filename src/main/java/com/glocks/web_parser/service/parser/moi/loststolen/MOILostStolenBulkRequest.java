package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.parser.moi.pendingverification.PendingVerificationRequest;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class MOILostStolenBulkRequest implements RequestTypeHandler<LostDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final WebActionDbRepository webActionDbRepository;
    private final MOIService moiService;
    private final FileOperations fileOperations;
    Map<String, String> map = new HashMap<>();
    private final AlertService alertService;
    private final MOILostStolenService moiLostStolenService;
    private final AppConfig appConfig;
    private final PendingVerificationRequest pendingVerificationRequest;

    public MOILostStolenBulkRequest(WebActionDbRepository webActionDbRepository, MOIService moiService, FileOperations fileOperations, AlertService alertService, MOILostStolenService moiLostStolenService, AppConfig appConfig, PendingVerificationRequest pendingVerificationRequest) {
        this.webActionDbRepository = webActionDbRepository;
        this.moiService = moiService;
        this.fileOperations = fileOperations;
        this.alertService = alertService;
        this.moiLostStolenService = moiLostStolenService;
        this.appConfig = appConfig;
        this.pendingVerificationRequest = pendingVerificationRequest;
    }

    @Override
    public void executeInitProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        executeValidateProcess(webActionDb, lostDeviceMgmt);
    }


    @Override
    public void executeValidateProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        String uploadedFileName = lostDeviceMgmt.getFileName();
        String transactionId = lostDeviceMgmt.getRequestId();
        String moiFilePath = appConfig.getMoiFilePath();
        String uploadedFilePath = moiFilePath + "/" + transactionId + "/" + uploadedFileName;
        logger.info("Uploaded file path is {}", uploadedFilePath);
        if (!fileOperations.checkFileExists(uploadedFilePath)) {
            logger.error("Uploaded file does not exists in path {} for lost ID {}", uploadedFilePath, transactionId);
            alertService.raiseAnAlert(transactionId, ConfigurableParameter.ALERT_LOST_STOLEN.getValue(), webActionDb.getSubFeature(), transactionId, 0);
            return;
        }
        if (!moiService.areHeadersValid(uploadedFilePath, "STOLEN", 9)) {
            moiService.updateStatusAsFailInLostDeviceMgmt(webActionDb, transactionId);
            return;
        }

        boolean isGreyListDurationValueValid = false;
        try {
            Integer.parseInt(moiService.greyListDuration());
            isGreyListDurationValueValid = true;
        } catch (Exception e) {
            logger.info("Invalid GREY_LIST_DURATION value");
        }
        if (isGreyListDurationValueValid) {
            map.put("uploadedFileName", uploadedFileName);
            map.put("transactionId", transactionId);
            map.put("uploadedFilePath", uploadedFilePath);
            map.put("moiFilePath", moiFilePath);
            executeProcess(webActionDb, lostDeviceMgmt);
        }
    }

    @Override
    public void executeProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
/*        logger.info("--------VERIFICATION PROCESS STARTED-------");
        logger.info("-------------------------------------------");
        pendingVerificationRequest.executeInitProcess(webActionDb, lostDeviceMgmt);*/
        logger.info("----STOLEN BULK PROCESS STARTED----");
        logger.info("-----------------------------------");
        String transactionId = map.get("transactionId");
        String processedFilePath = map.get("moiFilePath") + "/" + transactionId + "/" + transactionId + ".csv";
        logger.info("Processed file path is {}", processedFilePath);
        moiLostStolenService.fileProcess(webActionDb, lostDeviceMgmt, map.get("uploadedFileName"), map.get("uploadedFilePath"), Integer.parseInt(moiService.greyListDuration()));
        moiService.updateStatusInLostDeviceMgmt("Done", lostDeviceMgmt.getRequestId());
        webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
    }
}
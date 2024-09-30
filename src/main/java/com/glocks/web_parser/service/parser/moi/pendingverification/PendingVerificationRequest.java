package com.glocks.web_parser.service.parser.moi.pendingverification;

import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.parser.moi.loststolen.MOILostStolenBulkRequest;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PendingVerificationRequest implements RequestTypeHandler<LostDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final AppConfig appConfig;
    private final FileOperations fileOperations;
    private final WebActionDbRepository webActionDbRepository;
    private final PendingVerificationService pendingVerificationService;
    private final MOIService moiService;
    private final AlertService alertService;
    private final MOILostStolenBulkRequest moiLostStolenBulkRequest;
    Map<String, String> map = new HashMap<>();

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
            alertService.raiseAnAlert(transactionId, ConfigurableParameter.ALERT_PENDING_VERIFICATION.getValue(), "MOI Pending Verification", uploadedFileName + " with transaction id " + transactionId, 0);
            return;
        }
        map.put("uploadedFileName", uploadedFileName);
        map.put("transactionId", transactionId);
        map.put("uploadedFilePath", uploadedFilePath);
        map.put("moiFilePath", moiFilePath);
        executeProcess(webActionDb, lostDeviceMgmt);

    }

    @Override
    public void executeProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            verification(webActionDb, lostDeviceMgmt);
        }).thenRun(() -> {
            moiLostStolenBulkRequest.executeInitProcess(webActionDb, lostDeviceMgmt);
        });
        future.join();
    }

    public void verification(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        String transactionId = map.get("transactionId");
        String processedFilePath = map.get("moiFilePath") + "/" + transactionId + "/" + transactionId + ".csv";
        logger.info("Processed file path is {}", processedFilePath);
        PrintWriter printWriter = moiService.file(processedFilePath);
        boolean verificationStatus = pendingVerificationService.pendingVerificationFileValidation(map.get("uploadedFilePath"), lostDeviceMgmt, printWriter, ConfigurableParameter.PENDING_VERIFICATION_STAGE_INIT.getValue());
        logger.info("verificationStatus {}", verificationStatus);
        if (verificationStatus)
            pendingVerificationService.validFile(webActionDb, map.get("uploadedFilePath"), lostDeviceMgmt, printWriter, map.get("uploadedFileName"), ConfigurableParameter.PENDING_VERIFICATION_STAGE_DONE.getValue());
        else
            pendingVerificationService.invalidFile(webActionDb, map.get("uploadedFilePath"), lostDeviceMgmt, printWriter, map.get("uploadedFileName"), ConfigurableParameter.PENDING_VERIFICATION_STAGE_INIT.getValue());
    }
}
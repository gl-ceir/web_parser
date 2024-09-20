package com.glocks.web_parser.service.parser.MOI.IMEI_SEARCH_RECOVERY;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.dto.FileDto;
import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.parser.MOI.common.RequestTypeHandler;
import com.glocks.web_parser.validator.Validation;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class IMEISearchRecoveryBulkRequest implements RequestTypeHandler<SearchImeiByPoliceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final ImeiSearchRecoveryService imeiSearchRecoveryService;
    private final WebActionDbRepository webActionDbRepository;
    private final AppConfig appConfig;
    private final FileOperations fileOperations;


    @Override
    public void executeInitProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        executeValidateProcess(webActionDb, searchImeiByPoliceMgmt);
    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        String uploadedFileName = searchImeiByPoliceMgmt.getFileName();
        String transactionId = searchImeiByPoliceMgmt.getTransactionId();
        String uploadedFilePath = appConfig.getMoiFilePath() + "/" + transactionId + "/" + uploadedFileName;

        logger.info("Uploaded file path is {}", uploadedFilePath);
        if (!fileOperations.checkFileExists(uploadedFilePath)) {
            logger.error("Uploaded file does not exists in path {} for transactionId {}", uploadedFilePath,transactionId);
            return;
        }

        String processedFileName = transactionId + ".csv";
        String processedFilePath = appConfig.getMoiFilePath() + "/" + transactionId + "/" + processedFileName;
        logger.info("Processed file path is {}", processedFilePath);

        // create a file
        PrintWriter printWriterFroProcessFile = imeiSearchRecoveryService.file(processedFilePath);
        // validation on file
        if (imeiSearchRecoveryService.fileValidation(webActionDb.getTxnId(), uploadedFilePath, printWriterFroProcessFile))
            executeProcess(webActionDb, searchImeiByPoliceMgmt);

    }


    @Override
    public void executeProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        logger.info("executing process....");

    }
}

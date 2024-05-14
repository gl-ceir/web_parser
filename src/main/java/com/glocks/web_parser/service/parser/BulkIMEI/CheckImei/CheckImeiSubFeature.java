package com.glocks.web_parser.service.parser.BulkIMEI.CheckImei;

import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.builder.CheckImeiReqDetailBuilder;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.config.AppDbConfig;
import com.glocks.web_parser.config.DbConfigService;
import com.glocks.web_parser.constants.FileType;
import com.glocks.web_parser.constants.ListType;
import com.glocks.web_parser.dto.BulkCheckImeiDto;
import com.glocks.web_parser.dto.EmailDto;
import com.glocks.web_parser.dto.FileDto;

import com.glocks.web_parser.dto.RuleDto;
import com.glocks.web_parser.model.app.BulkCheckImeiMgmt;

import com.glocks.web_parser.model.app.CheckImeiReqDetail;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.email.EmailService;
import com.glocks.web_parser.service.fileCopy.ListFileManagementService;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.parser.BulkIMEI.UtilFunctions;
import com.glocks.web_parser.service.rule.Rules;
import com.glocks.web_parser.validator.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.Connection;
import java.util.List;

import static com.glocks.web_parser.constants.BulkCheckImeiConstants.*;
import static com.glocks.web_parser.constants.ConfigFlag.msgForCompliantBulkImei;

@Service
public class CheckImeiSubFeature {

    @Autowired
    WebActionDbRepository webActionDbRepository;
    @Autowired
    FileOperations fileOperations;
    @Autowired
    AppConfig appConfig;
    @Autowired
    SysParamRepository sysParamRepository;
    @Autowired
    BulkCheckImeiMgmtRepository bulkCheckImeiMgmtRepository;
    @Autowired
    UtilFunctions utilFunctions;
    @Autowired
    RuleRepository ruleRepository;
    @Autowired
    Rules rules;
    @Autowired
    AppDbConfig appDbConfig;
    @Autowired
    Validation validation;
    @Autowired
    CheckImeiReqDetailRepository checkImeiReqDetailRepository;
    @Autowired
    EmailService emailService;
    @Autowired
    EirsResponseParamRepository eirsResponseParamRepository;
    @Autowired
    ListFileManagementService listFileManagementService;
    @Autowired
    DbConfigService dbConfigService;
    @Autowired
    AlertService alertService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void executeInitProcess(WebActionDb webActionDb, BulkCheckImeiMgmt bulkCheckImeiMgmt) {
        logger.info("Starting the init process for bulk check IMEI for transaction id {}", webActionDb.getTxnId());

        webActionDbRepository.updateWebActionStatus(2, webActionDb.getId());
        executeValidateProcess(webActionDb, bulkCheckImeiMgmt);

    }

    public void executeValidateProcess(WebActionDb webActionDb, BulkCheckImeiMgmt bulkCheckImeiMgmt) {
        logger.info("Starting the validate process for bulk check IMEI for transaction id {}", webActionDb.getTxnId());
        EmailDto emailDto = new EmailDto();
        emailDto.setEmail(bulkCheckImeiMgmt.getEmail());
        emailDto.setTxn_id(bulkCheckImeiMgmt.getTransactionId());
        String language = bulkCheckImeiMgmt.getLanguage() == null ?
                sysParamRepository.getValueFromTag("systemDefaultLanguage") :  bulkCheckImeiMgmt.getLanguage();
        emailDto.setLanguage(language);
        //dbConfigService.loadAllConfig();
        try {

            String currentFileName = bulkCheckImeiMgmt.getFileName();
            String transactionId = bulkCheckImeiMgmt.getTransactionId();
            String filePath = appConfig.getBulkCheckImeiFilePath() + "/" + transactionId + "/" + currentFileName;
            FileDto currFile = new FileDto(currentFileName, appConfig.getListMgmtFilePath() + "/" + bulkCheckImeiMgmt.getTransactionId());
            emailDto.setFile(filePath);
            logger.info("File path is {}", filePath);
            if(!fileOperations.checkFileExists(filePath)) {
                logger.error("File does not exists {}", filePath);
                alertService.raiseAnAlert("alert6001", "Bulk Check IMEI", currentFileName, 0);
//                utilFunctions.updateFailStatus(webActionDb, bulkCheckImeiMgmt);
                return ;
            }
            if(currFile.getTotalRecords() > Integer.parseInt(sysParamRepository.getValueFromTag("BULK_CHECK_IMEI_COUNT"))) {
                emailDto.setSubject(eirsResponseParamRepository.findValue(featureName,
                        language, numberOfRecordsSubject));
                emailDto.setMessage(eirsResponseParamRepository.findValue(featureName,
                        language, numberOfRecordsMessage));
//                emailDto.setSubject(dbConfigService.getValue("numberOfRecordsSubject"));
//                emailDto.setSubject(dbConfigService.getValue("numberOfRecordsMessage"));
                emailService.callEmailApi(emailDto);
                utilFunctions.updateFailStatus(webActionDb, bulkCheckImeiMgmt);
                return ;
            }
            if(!fileValidation(filePath)) {
                // send email as well
                emailDto.setSubject(eirsResponseParamRepository.findValue(featureName,
                        language, invalidDataFormatSubject));
                emailDto.setMessage(eirsResponseParamRepository.findValue(featureName,
                        language, invalidDataFormatMessage));
//                emailDto.setSubject(dbConfigService.getValue("invalidDataFormatSubject"));
//                emailDto.setSubject(dbConfigService.getValue("invalidDataFormatMessage"));
                emailService.callEmailApi(emailDto);
                utilFunctions.updateFailStatus(webActionDb, bulkCheckImeiMgmt);
                return ;
            }
            logger.info("File is ok will process it now");
            webActionDbRepository.updateWebActionStatus(3, webActionDb.getId());
            executeProcess(webActionDb, bulkCheckImeiMgmt);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

    }

    public void executeProcess(WebActionDb webActionDb, BulkCheckImeiMgmt bulkCheckImeiMgmt) {

        logger.info("Starting the execute process for bulk imei check");
        EmailDto emailDto = new EmailDto();
        emailDto.setEmail(bulkCheckImeiMgmt.getEmail());
        emailDto.setTxn_id(bulkCheckImeiMgmt.getTransactionId());
        String language = bulkCheckImeiMgmt.getLanguage() == null ?
                sysParamRepository.getValueFromTag("systemDefaultLanguage") :  bulkCheckImeiMgmt.getLanguage();
        emailDto.setLanguage(language);
        try {
            String currentFileName = bulkCheckImeiMgmt.getFileName();
            String transactionId = bulkCheckImeiMgmt.getTransactionId();
//            String filePath = appConfig.getBulkCheckImeiFilePath() + "/" + transactionId + "/" + currentFileName;
            FileDto currFile = new FileDto(currentFileName, appConfig.getBulkCheckImeiFilePath() + "/" + bulkCheckImeiMgmt.getTransactionId());
            File outFile = new File(appConfig.getBulkCheckImeiFilePath() + "/" + transactionId
                    + "/" + transactionId+".csv");
            PrintWriter writer = new PrintWriter(outFile);
            List<RuleDto> ruleList = ruleRepository.getRuleDetails("BULK_CHECK_IMEI", "Enabled");
            logger.info(ruleList.toString());
            boolean output = fileRead(currFile, ruleList, writer, bulkCheckImeiMgmt);
            writer.close();
            listFileManagementService.saveListManagementEntity(transactionId, ListType.CHECKIMEIBULK, FileType.BULK,
                    appConfig.getBulkCheckImeiFilePath() + "/" + bulkCheckImeiMgmt.getTransactionId() + "/",
                    bulkCheckImeiMgmt.getTransactionId() + ".csv", currFile.getTotalRecords());
            if(!output) {
                logger.error("Updating with fail status");
//                emailService.callEmailApi(emailDto);
                utilFunctions.updateFailStatus(webActionDb, bulkCheckImeiMgmt);
                return;
            }
            logger.info("The file processed successfully, updating success status.");
            emailDto.setFile(appConfig.getBulkCheckImeiFilePath() + "/" + transactionId
                    + "/" + transactionId+".csv");
            emailDto.setSubject(eirsResponseParamRepository.findValue(featureName,
                    language, fileProcessSuccessSubject));
            emailDto.setMessage(eirsResponseParamRepository.findValue(featureName,
                    language, fileProcessSuccessMessage));
//            emailDto.setSubject(dbConfigService.getValue("fileProcessSuccessSubject"));
//            emailDto.setSubject(dbConfigService.getValue("fileProcessSuccessMessage"));
            emailService.callEmailApi(emailDto);
            utilFunctions.updateSuccessStatus(webActionDb, bulkCheckImeiMgmt);
        } catch (Exception e) {
            logger.error("Exception while processing the file: {}", bulkCheckImeiMgmt.getFileName());
        }
    }

    public boolean fileRead(FileDto file, List<RuleDto> ruleList, PrintWriter outFile, BulkCheckImeiMgmt bulkCheckImeiMgmt) {
        int successCount = 0;
        int failureCount = 0;
        boolean gracePeriod = rules.checkGracePeriod();
        try(BufferedReader reader = new BufferedReader(new FileReader(file.getFilePath()+"/"+file.getFileName()))
        ) {
            Connection conn = appDbConfig.springDataSource().getConnection();
            String record = null;
            try {
                while((record = reader.readLine()) != null) {
                    if(record.isEmpty()) continue;

                    String[] bulkCheckImeiRecord = record.split(",", -1);
                    BulkCheckImeiDto bulkCheckImeiDto = new BulkCheckImeiDto(bulkCheckImeiRecord);
                    String ruleOutput = rules.applyRule(ruleList, bulkCheckImeiDto.getImei(), gracePeriod, conn);
                    CheckImeiReqDetail checkImeiReqDetail = CheckImeiReqDetailBuilder.forInsert(
                            bulkCheckImeiDto.getImei(), bulkCheckImeiMgmt);
                    if(ruleOutput.isEmpty() || ruleOutput.isBlank()) {
                        successCount++;
                        outFile.println(record + ","+ dbConfigService.getValue("msgForCompliantBulkImei"));
                        checkImeiReqDetail.setComplianceStatus(dbConfigService.getValue("msgForCompliantBulkImei"));
                        checkImeiReqDetailRepository.save(checkImeiReqDetail);
                    } else {
                        failureCount++;
                        outFile.println(record + "," + dbConfigService.getValue("msgForNonCompliantBulkImei"));
                        checkImeiReqDetail.setComplianceStatus(dbConfigService.getValue("msgForNonCompliantBulkImei"));
                        checkImeiReqDetailRepository.save(checkImeiReqDetail);
                    }
                }
            } catch (Exception ex) {
                logger.error("Exception in processing the record {}", record);
                outFile.println(record);
            }
            reader.close();
        }  catch (Exception ex) {
            logger.error("Exception in processing the file {}", file.getFileName());
            return false;
        }
        logger.info("File summary is {}", file);
        file.setFailedRecords(failureCount);
        file.setSuccessRecords(successCount);
        return true;
    }

    boolean fileValidation(String fileName) {
        File file = new File(fileName);
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String record;
            while((record = reader.readLine()) != null) {
                String[] imeiRecord = record.split(",", -1);
                String imei = imeiRecord[0].trim();
                if(imeiRecord.length != 1 || !validation.isLengthEqual(imei, 15) || !validation.isNumeric(imei)) {
                    logger.error("The record {} is not in correct format {}", record);
                    return false;
                }
            }
            reader.close();
            logger.error("The file is validated and is matches the required format");
            return true;
        } catch (Exception ex) {
            logger.error("Exception while reading the file {} {}", fileName, ex.getMessage());
            return false;
        }
    }

}



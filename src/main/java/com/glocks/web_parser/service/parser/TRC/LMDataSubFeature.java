package com.glocks.web_parser.service.parser.TRC;


import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.config.AppDbConfig;
import com.glocks.web_parser.dto.FileDto;
import com.glocks.web_parser.dto.RuleDto;
import com.glocks.web_parser.dto.TrcLocalManufacturerDto;
import com.glocks.web_parser.model.app.TrcDataMgmt;
import com.glocks.web_parser.model.app.TrcLocalManufacturedDevice;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.rule.Rules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LMDataSubFeature {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    AppConfig appConfig;
    @Autowired
    TrcLocalManufacturedDeviceRepository trcLocalManufacturedDeviceRepository;
    @Autowired
    WebActionDbRepository webActionDbRepository;
    @Autowired
    TrcDataMgmtRepository trcDataMgmtRepository;

    @Autowired
    FileOperations fileOperations;

    @Autowired
    RuleRepository ruleRepository;

    @Autowired
    AlertService alertService;
    @Autowired
    Rules rules;
    @Autowired
    SysParamRepository sysParamRepository;
    @Autowired
    AppDbConfig appDbConfig;

    HashMap<String, String> hashMap = new HashMap<>();


    void initProcess(WebActionDb webActionDb) {
        logger.info("Starting the init function for TRC, TA-Data sub feature {}", webActionDb);
        // changing the status in web action db to 2
        hashMap = new HashMap<>();
        webActionDbRepository.updateWebActionStatus(2, webActionDb.getId());
        validateProcess(webActionDb);
    }

    void validateProcess(WebActionDb webActionDb) {
        logger.info("Validating the file for local manufacturer dump");
        try {
            // only check the header and move next
            TrcDataMgmt trcDataMgmt = trcDataMgmtRepository.findByTransactionId(webActionDb.getTxnId());
            logger.info("The trc data management entry is {}", trcDataMgmt);
            String currentFileName = trcDataMgmt.getFileName();
            String transactionId = trcDataMgmt.getTransactionId();
            String filePath = appConfig.getLocalManufacturerBaseFilePath() + "/" + transactionId + "/" + currentFileName;
            FileDto currFile = new FileDto(currentFileName, appConfig.getLocalManufacturerBaseFilePath() + "/" + trcDataMgmt.getTransactionId());

            logger.info("File path is {}", filePath);
            if(!fileOperations.checkFileExists(filePath)) {
                logger.error("File does not exist");
                updateFailStatus(webActionDb, trcDataMgmt, "Some internal problem, please try after some time.", "alert6001", "LM", currentFileName);
                return ;
            }
            if(currFile.getTotalRecords() > Integer.parseInt(sysParamRepository.getValueFromTag("LM_FILE_COUNT"))) {
                updateFailStatus(webActionDb, trcDataMgmt, "Total number of records in file is greater than the limit", "alert6002", "LM", currentFileName);
//                fileOperations.moveFile(currentFileName, currentFileName, appConfig.getLocalManufacturerBaseFilePath() + "/" +
//                        transactionId, appConfig.getLocalManufacturerProcessedBaseFilePath() + "/" + transactionId);
                return ;
            }
            if(!fileValidation(filePath)) {
                updateFailStatus(webActionDb, trcDataMgmt, "Data format is not valid, please check sample file.", "alert6002", "LM", currentFileName);
//                fileOperations.moveFile(currentFileName, currentFileName, appConfig.getLocalManufacturerBaseFilePath() + "/" +
//                        transactionId, appConfig.getLocalManufacturerProcessedBaseFilePath() + "/" + transactionId);
                return ;
            }
            File outFile = new File(appConfig.getLocalManufacturerBaseFilePath() + "/" + transactionId
                    + "/" + currentFileName+"_processed");
            PrintWriter writer = new PrintWriter(outFile);
            List<RuleDto> ruleList = ruleRepository.getRuleDetails("LM", "Enabled");
            logger.info(ruleList.toString());
            boolean output = fileRead(currFile, ruleList, writer);
            writer.close();
            if(!output) {
                updateFailStatus(webActionDb, trcDataMgmt, "Some entries failed validation. Please recheck the file", "alert6002", "LM", currentFileName);
                return;
            }
            logger.info("File is ok will process it now");
            webActionDbRepository.updateWebActionStatus(3, webActionDb.getId());
            executeProcess(webActionDb);
        } catch (Exception ex) {
            logger.error(ex.getMessage());

        }
    }

    void executeProcess(WebActionDb webActionDb) {
        try {
            TrcDataMgmt trcDataMgmt = trcDataMgmtRepository.findByTransactionId(webActionDb.getTxnId());
            String currentFileName = trcDataMgmt.getFileName();
            String transactionId = trcDataMgmt.getTransactionId();
            String filePath = appConfig.getLocalManufacturerBaseFilePath() + "/" + transactionId + "/" + currentFileName;
            FileDto currFile = new FileDto(currentFileName, appConfig.getLocalManufacturerBaseFilePath() + "/" + trcDataMgmt.getTransactionId());
            processFile(currFile);
            logger.info("File processed. {}", currFile);
            updateSuccessStatus(webActionDb, trcDataMgmt, "The file processed successfully.");
//            fileOperations.moveFile();
        } catch (Exception ex) {
            logger.error("Exception is {}", ex.getMessage());
        }
    }
    boolean fileValidation(String fileName) {
        File file = new File(fileName);
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headers = reader.readLine();
            String[] header = headers.split(appConfig.getTrcLocalManufacturerFileSeparator(), -1);
            if(header.length != 5) {
                return false;
            }
            TrcLocalManufacturerDto trcLocalManufacturerDto = new TrcLocalManufacturerDto(header);
            if (trcLocalManufacturerDto.getImei().trim().equalsIgnoreCase("imei") &&
                    trcLocalManufacturerDto.getSerialNumber().trim().equalsIgnoreCase("serial number") &&
                    trcLocalManufacturerDto.getManufacturerId().trim().equalsIgnoreCase("manufacturer id") &&
                    trcLocalManufacturerDto.getManufacturerName().trim().equalsIgnoreCase("manufacturer name") &&
                    trcLocalManufacturerDto.getManufactureringDate().trim().equalsIgnoreCase("Manufacturing date")

            ) {
                reader.close();
                return true;
            }
            reader.close();
            logger.error("The header of the file is not correct");
            return false;
        } catch (Exception ex) {
            logger.error("Exception while reading the file {} {}", fileName, ex.getMessage());
            return false;
        }
    }

    public boolean fileRead(FileDto file, List<RuleDto> ruleList, PrintWriter outFile) {
        int successCount = 0;
        int failureCount = 0;
        boolean gracePeriod = rules.checkGracePeriod();
        hashMap = new HashMap<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(file.getFilePath()+"/"+file.getFileName()))
        ) {
            Connection conn = appDbConfig.springDataSource().getConnection();
            String record = null;
            outFile.println(reader.readLine()+",Status,Reason");
            try {
                while((record = reader.readLine()) != null) {
                    if(record.isEmpty()) continue;

                    String[] lmDataRecord = record.split(appConfig.getTrcLocalManufacturerFileSeparator(), -1);
                    if(lmDataRecord.length != 5) {
                        logger.error("The record length is not equal to 5 {}",  record);
                        failureCount++;
                        outFile.println(record + ",NOT-OK" + "," + "Record Length Error");
                        continue;
                    }
                    if(lmDataRecord[0].length() < 14) {
                        logger.error("The imei in record {} is less than 14", record);
                        failureCount++;
                        outFile.println(record + ",NOT-OK" + "," + "IMEI length Error.");
                        continue;
                    }
                    TrcLocalManufacturerDto trcLocalManufacturerDto = new TrcLocalManufacturerDto(lmDataRecord);
                    if(hashMap.containsKey(trcLocalManufacturerDto.getImei().substring(0,14))) {
                        logger.error("The record with same IMEI already exists: {}", trcLocalManufacturerDto);
                        failureCount++;
                        outFile.println(record + ",NOT-OK" + "," + "IMEI already exist");
                        continue;
                    }
                    TrcLocalManufacturedDevice trcLocalManufacturedDevice = new TrcLocalManufacturedDevice(lmDataRecord);
                    String ruleOutput = rules.applyRule(ruleList, trcLocalManufacturedDevice, gracePeriod, conn);
                    if(ruleOutput.isEmpty() || ruleOutput.isBlank()) {
                        successCount++;
                        outFile.println(record + ",OK" + "," + "NA");
                    } else {
                        failureCount++;
                        outFile.println(record + ",NOT-OK" + "," + ruleOutput);
                    }
                    hashMap.put(trcLocalManufacturerDto.getImei().substring(0,14), record);
                }
            } catch (Exception ex) {
                logger.error("Exception in processing the record {}", record);
            }
            reader.close();
        }  catch (Exception ex) {
                logger.error("Exception in processing the file {}", file.getFileName());
        }

        if(failureCount == 0) {
            logger.info("All entries passed the checks, will process all the entries");
//            processFile(file);
        }
        else {
            logger.error("Some entries failed the check for the file {}" , file.getFileName());
            file.setFailedRecords(failureCount);
            file.setSuccessRecords(successCount);
            return false;
        }
        logger.info("File summary is {}", file);
        return true;
    }

    public void processFile(FileDto file) {
        int successCount = 0;
        int failureCount = 0;
        logger.info("Hash size is {}: ", hashMap.size());
        if(hashMap.isEmpty()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file.getFilePath() + "/" + file.getFileName()))) {
                String record = null;
                reader.readLine();
                try {
                    while ((record = reader.readLine()) != null) {
                        if (record.isEmpty()) continue;
                        String[] lmRecord = record.split(appConfig.getTrcLocalManufacturerFileSeparator(), -1);
                        TrcLocalManufacturedDevice trcLocalManufacturedDevice = new TrcLocalManufacturedDevice(lmRecord);
                        logger.info("Entry save from file {}", trcLocalManufacturedDevice);
                        trcLocalManufacturedDeviceRepository.save(trcLocalManufacturedDevice);
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.error("Exception occurred while inserting in DB for local manufacture {} with error {}",
                            record, e.getMessage());
                }
                reader.close();
            } catch (Exception e) {
                logger.error("Exception while processing the file for local manufacturer {} while inserting in DB {}",
                        file.getFileName(), e.getMessage());
            }
            file.setSuccessRecords(successCount);
            return;
        }
        else {
            try {
                for (Map.Entry<String, String> entry : hashMap.entrySet()) {
                    String[] lmRecord = entry.getValue().split(appConfig.getTrcLocalManufacturerFileSeparator(), -1);
                    TrcLocalManufacturedDevice trcLocalManufacturedDevice = new TrcLocalManufacturedDevice(lmRecord);
                    try {
                        logger.info("Entry save from hash {}", trcLocalManufacturedDevice);
                        trcLocalManufacturedDeviceRepository.save(trcLocalManufacturedDevice);
                        successCount++;
                    } catch (Exception e) {
                        logger.error("Exception occurred while inserting in DB for local manufacture {} with error {}",
                                lmRecord, e.getMessage());
                    }
                }

            }
            catch (Exception e) {
                logger.error("Exception while processing the file for local manufacturer {} while inserting in DB {}",
                        file.getFileName(), e.getMessage());
            }
            file.setSuccessRecords(successCount);
        }
    }

    void updateFailStatus(WebActionDb webActionDb, TrcDataMgmt trcDataMgmt, String remarks, String alertId,
                          String type, String fileName) {
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        trcDataMgmtRepository.updateTrcDataMgmtStatus("FAIL", LocalDateTime.now(), remarks,trcDataMgmt.getId());
        alertService.raiseAnAlert(alertId, type, fileName, 0);
    }


    void updateSuccessStatus(WebActionDb webActionDb, TrcDataMgmt trcDataMgmt, String remarks) {
        webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
        trcDataMgmtRepository.updateTrcDataMgmtStatus("DONE", LocalDateTime.now(), remarks,trcDataMgmt.getId());
    }


}

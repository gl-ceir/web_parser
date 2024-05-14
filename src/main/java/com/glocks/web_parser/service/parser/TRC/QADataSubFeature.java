package com.glocks.web_parser.service.parser.TRC;


import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.config.DbConfigService;
import com.glocks.web_parser.constants.FileType;
import com.glocks.web_parser.constants.ListType;
import com.glocks.web_parser.dto.FileDto;
import com.glocks.web_parser.dto.TrcQaFileDto;
import com.glocks.web_parser.model.app.TrcDataMgmt;
import com.glocks.web_parser.model.app.TrcQualifiedAgentsData;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.TrcDataMgmtRepository;
import com.glocks.web_parser.repository.app.TrcQualifiedAgentsDataRepository;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.fileCopy.ListFileManagementService;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static com.glocks.web_parser.constants.Constants.done;

@Service
public class QADataSubFeature {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    AppConfig appConfig;
    @Autowired
    WebActionDbRepository webActionDbRepository;
    @Autowired
    TrcDataMgmtRepository trcDataMgmtRepository;
    @Autowired
    AlertService alertService;
    @Autowired
    TrcQualifiedAgentsDataRepository trcQualifiedAgentsDataRepository;
    @Autowired
    FileOperations fileOperations;
    @Autowired
    ListFileManagementService listFileManagementService;
    @Autowired
    DbConfigService dbConfigService;

    String sortedFileName = "sortedFile.csv";

    void initProcess(WebActionDb webActionDb) {
        logger.info("Starting the init function for TRC, QA-Data sub feature {}", webActionDb);
        // changing the status in web action db to 2
        webActionDbRepository.updateWebActionStatus(2, webActionDb.getId());
        validateProcess(webActionDb);
    }

    void validateProcess(WebActionDb webActionDb) {

        logger.info("Validating the files.");
        // validating the file recd.
        try {
            TrcDataMgmt trcDataMgmt = trcDataMgmtRepository.findByTransactionId(webActionDb.getTxnId());
            logger.info("The trc data management entry is {}", trcDataMgmt);
            String currentFileName = trcDataMgmt.getFileName();
            String transactionId = trcDataMgmt.getTransactionId();
            String filePath = appConfig.getQaBaseFilePath() + "/" + transactionId + "/" + currentFileName;
            logger.info("File path is {}", filePath);
            String date = webActionDb.getModifiedOn().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            logger.info("Date is {}", date);
            String deltaDeleteFile = appConfig.getQaBaseFilePath() + "/" + trcDataMgmt.getTransactionId() + "/" +
                    "trc_data_qa_dump_del_"+date+".csv";
            String deltaAddFile = appConfig.getQaBaseFilePath() + "/" + trcDataMgmt.getTransactionId() + "/" +
                    "trc_data_qa_dump_add_"+date+".csv";
            if(!fileOperations.checkFileExists(filePath)) {
                logger.error("File does not exists {}", filePath);
                alertService.raiseAnAlert("alert6001", "QA", currentFileName, 0);
//                updateFailStatus(webActionDb, trcDataMgmt, dbConfigService.getValue("msgForRemarksForInternalErrorInQA"), "alert6001", "QA", currentFileName);
                return ;
            }

            logger.info("{} {}",appConfig.getQaBaseFilePath(), currentFileName);
            FileDto currFile = new FileDto(currentFileName, appConfig.getQaBaseFilePath()+"/"+trcDataMgmt.getTransactionId());



            logger.info("File {} exists on the path {}", currentFileName,
                    appConfig.getQaBaseFilePath() + "/" + transactionId);
            if(!fileValidation(filePath)) {
                updateFailStatus(webActionDb, trcDataMgmt, dbConfigService.getValue("msgForRemarksForDataFormatErrorInQA"), "alert6002", "QA", currentFileName);
//                fileOperations.moveFile(currentFileName, currentFileName, appConfig.getQaBaseFilePath() + "/" +
//                        transactionId, appConfig.getQaProcessedBaseFilePath() + "/" + transactionId);
                return ;
            }
            // pick the last successfully processed file
            TrcDataMgmt previousTrcDataMgmt = trcDataMgmtRepository.getFileName(done, "QA");
            // sort the current file

//            String sortedFileName = "trc_data_qa_dump_sorted_"+date+".csv";
            String sortedFilePath = appConfig.getQaBaseFilePath() + "/" + transactionId + "/" +currentFileName+"_sorted";
//            String sortedFilePath = appConfig.getQaBaseFilePath() + "/" + transactionId + "/" +sortedFileName;
            logger.info("Sorted file is {}" ,sortedFilePath );
            if(!fileOperations.sortFile(filePath,sortedFilePath)) {
                alertService.raiseAnAlert("alert6003", "while sorting file for TRC QA", currentFileName, 0);
                return ;
            }

            if(previousTrcDataMgmt == null) {
                logger.info("No previous file exists for QA data. Taking file as fresh file {}", currentFileName);
                // copy the contents of current file as it is in add file but sort the file.
                boolean output = fileOperations.copy(currFile, deltaAddFile, deltaDeleteFile);
                if(!output) {
                    alertService.raiseAnAlert("alert6003", "while creating diff file for TRC QA", currentFileName, 0);
                    return ;
                }
            }
            else {
                // check if previous file exists or not....
                String previousProcessedFilePath = appConfig.getQaBaseFilePath() + "/" +
                        previousTrcDataMgmt.getTransactionId() +"/" + previousTrcDataMgmt.getFileName() + "_sorted";

                if(!fileOperations.checkFileExists(previousProcessedFilePath)) {
                    logger.error("No previous file exists for QA data.");
                    updateFailStatus(webActionDb, trcDataMgmt, dbConfigService.getValue("msgForRemarksForInternalErrorInQA"), "alert6001", "QA", previousTrcDataMgmt.getFileName());
                    return;
                }
                // create diff
                if(fileOperations.createDiffFiles(sortedFilePath, previousProcessedFilePath, deltaDeleteFile, 0)) {
                    alertService.raiseAnAlert("alert6003", "while creating diff file for TRC QA", currentFileName, 0);

                    return ;
                }

                if(fileOperations.createDiffFiles(sortedFilePath, previousProcessedFilePath, deltaAddFile, 1)) {
                    alertService.raiseAnAlert("alert6003", "while creating diff file for TRC QA", currentFileName, 0);
                    return ;
                }
                logger.info("Diff file creation successful");

            }
//            fileOperations.moveFile(currentFileName, currentFileName, appConfig.getQaBaseFilePath() + "/" +
//                    transactionId, appConfig.getQaProcessedBaseFilePath() + "/" + transactionId);
            // all done updating the entry to 3 in web action db and calling process file functions
            webActionDbRepository.updateWebActionStatus(3, webActionDb.getId());
            executeProcess(webActionDb);
            listFileManagementService.saveListManagementEntity(transactionId, ListType.QADATA, FileType.PROCESSED_FILE, appConfig.getQaBaseFilePath() + "/" +
                            transactionId +"/", currentFileName+"_sorted",(long) currFile.getTotalRecords());

            listFileManagementService.saveListManagementEntity(transactionId, ListType.QADATA, FileType.PROCESSED_FILE, appConfig.getQaBaseFilePath() + "/" +
                            transactionId +"/", "trc_data_qa_dump_del_"+date+".csv", 0L);

            listFileManagementService.saveListManagementEntity(transactionId, ListType.QADATA, FileType.PROCESSED_FILE, appConfig.getQaBaseFilePath() + "/" +
                    transactionId +"/", "trc_data_qa_dump_add_"+date+".csv", 0L);

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    void executeProcess(WebActionDb webActionDb) {
        TrcDataMgmt trcDataMgmt = trcDataMgmtRepository.findByTransactionId(webActionDb.getTxnId());
        String transactionId = trcDataMgmt.getTransactionId();
        String date = webActionDb.getModifiedOn().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        FileDto delFile = new FileDto("trc_data_qa_dump_del_"+ date+".csv",
                appConfig.getQaBaseFilePath() + "/"+trcDataMgmt.getTransactionId());
        FileDto addFile = new FileDto("trc_data_qa_dump_add_"+ date+".csv",
                appConfig.getQaBaseFilePath() + "/"+trcDataMgmt.getTransactionId());
        try {

            boolean output1 = fileRead(delFile, 1);
            if(!output1) {
                logger.error("Error in processing delete delta file for TRC QA data.");
                updateFailStatus(webActionDb, trcDataMgmt, dbConfigService.getValue("msgForRemarksForInternalErrorInQA"), "alert6003", "while processing delete diff file for TRC QA", delFile.getFileName());
//
//                fileOperations.moveFile(delFile.getFileName(), delFile.getFileName(), appConfig.getQaBaseFilePath() + "/" +
//                        transactionId, appConfig.getQaProcessedBaseFilePath() + "/" + transactionId);
//                fileOperations.moveFile(addFile.getFileName(), addFile.getFileName(), appConfig.getQaBaseFilePath() + "/" +
//                        transactionId, appConfig.getQaProcessedBaseFilePath() + "/" + transactionId);
                return;
            }
            boolean output2 = fileRead(addFile, 0);
            if(!output2) {
                logger.error("Error in processing delete add file for TRC QA data");
                updateFailStatus(webActionDb, trcDataMgmt, dbConfigService.getValue("msgForRemarksForInternalErrorInQA"), "alert6003", "while processing delete diff file for TRC QA", delFile.getFileName());
//                fileOperations.moveFile(delFile.getFileName(), delFile.getFileName(), appConfig.getQaBaseFilePath() + "/" +
//                        transactionId, appConfig.getQaProcessedBaseFilePath() + "/" + transactionId);
//                fileOperations.moveFile(addFile.getFileName(), addFile.getFileName(), appConfig.getQaBaseFilePath() + "/" +
//                        transactionId, appConfig.getQaProcessedBaseFilePath() + "/" + transactionId);
                return;
            }
            logger.info("Delete delta file summary for TRC QA data: {}", delFile);
            logger.info("Add delta file summary for TRC QA data: {}", addFile);

            updateSuccessStatus(webActionDb, trcDataMgmt, dbConfigService.getValue("msgForRemarksForSuccessInQA"));
//            fileOperations.moveFile(delFile.getFileName(), delFile.getFileName(), appConfig.getQaBaseFilePath() + "/" +
//                    transactionId, appConfig.getQaProcessedBaseFilePath() + "/" + transactionId);
//            fileOperations.moveFile(addFile.getFileName(), addFile.getFileName(), appConfig.getQaBaseFilePath() + "/" +
//                    transactionId, appConfig.getQaProcessedBaseFilePath() + "/" + transactionId);

        } catch (Exception ex) {
            logger.error("Error in executing the process for delta files for QA data");
//            updateFailStatus(webActionDb, trcDataMgmt, "Some internal problem, please try after some time.");
            return ;
        }
    }

    boolean fileRead(FileDto fileDto, int request) {
        // read file and process the entries
        int failureCount=0;
        int succesCount=0;
        try(BufferedReader reader = new BufferedReader(new FileReader(fileDto.getFilePath() +"/" + fileDto.getFileName()))) {

            try {
                String record;
                reader.readLine(); // skipping the header
                while ((record = reader.readLine()) != null) {
                    if (record.isEmpty()) {
                        continue;
                    }

                    String[] taDataRecord = record.split(appConfig.getTrcQaFileSeparator(), -1);
                    logger.info("Record length {}", taDataRecord.length);
                    if(taDataRecord.length != 4) {
                        logger.error("The record length is not equal to 4 {}", Arrays.stream(taDataRecord));
                        continue;
                    }

                    TrcQualifiedAgentsData taData = new TrcQualifiedAgentsData(taDataRecord);
                    try {
                        if(request == 0) {
                            logger.info("Inserting the entry {}", taData);
                            trcQualifiedAgentsDataRepository.save(taData);
                        }
                        else {
                            logger.info("Deleting the the entry {}", taData);
                            trcQualifiedAgentsDataRepository.deleteByEmail(taData.getEmail());
                        }
                        succesCount++;

                    } catch (Exception ex) {
                        if(request == 0 ) logger.error("The entry failed to save in QA Data, {}", taData);
                        else logger.error("The entry failed to delete in QA Data, {}", taData);
                        logger.error(ex.toString());
                        failureCount++;
                    }
                }
            } catch (Exception ex) {
                logger.error("File processing for file {}, failed due to {}", fileDto.getFileName(), ex.getMessage());
                fileDto.setFailedRecords(failureCount);
                fileDto.setSuccessRecords(succesCount);
                return false;
            }

        } catch (FileNotFoundException ex) {
            logger.error("File processing for file {}, failed due to {}", fileDto.getFileName(), ex.getMessage());
            fileDto.setFailedRecords(failureCount);
            fileDto.setSuccessRecords(succesCount);
            return false;
        } catch (IOException ex) {
            logger.error("File processing for file {}, failed due to {}", fileDto.getFileName(), ex.getMessage());
            fileDto.setFailedRecords(failureCount);
            fileDto.setSuccessRecords(succesCount);
            return false;
        } catch (Exception ex) {
            logger.error("File processing for file {}, failed due to {}", fileDto.getFileName(), ex.getMessage());
            fileDto.setFailedRecords(failureCount);
            fileDto.setSuccessRecords(succesCount);
            return false;
        }
        fileDto.setFailedRecords(failureCount);
        fileDto.setSuccessRecords(succesCount);
        return true;
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

    boolean fileValidation(String fileName) {
        File file = new File(fileName);
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headers = reader.readLine();
            String[] header = headers.split(appConfig.getTrcQaFileSeparator(), -1);
            if(header.length != 4) {
                return false;
            }
            TrcQaFileDto trcQaFileDto = new TrcQaFileDto(header);
            if (trcQaFileDto.getNo().trim().equalsIgnoreCase("no.") &&
                    trcQaFileDto.getCompanyName().trim().equalsIgnoreCase("company name") &&
                    trcQaFileDto.getPhoneNumber().trim().equalsIgnoreCase("phone number") &&
                    trcQaFileDto.getEmail().trim().equalsIgnoreCase("email")
            ) {
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


}

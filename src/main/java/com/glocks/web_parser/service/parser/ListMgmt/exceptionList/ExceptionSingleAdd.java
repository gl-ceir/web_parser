package com.glocks.web_parser.service.parser.ListMgmt.exceptionList;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.*;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.operatorSeries.OperatorSeriesService;
import com.glocks.web_parser.service.parser.ListMgmt.CommonFunctions;
import com.glocks.web_parser.validator.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;

@Service
public class ExceptionSingleAdd implements IRequestTypeAction {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    WebActionDbRepository webActionDbRepository;
    @Autowired
    Validation validation;
    @Autowired
    AppConfig appConfig;
    @Autowired
    FileOperations fileOperations;
    @Autowired
    OperatorSeriesService operatorSeriesService;
    @Autowired
    CommonFunctions commonFunctions;

    @Override
    public  void executeInitProcess(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {
        logger.info("Starting the init process for exception list, for request {} and action {}",
                listDataMgmt.getRequestMode(), listDataMgmt.getAction());

        webActionDbRepository.updateWebActionStatus(2, webActionDb.getId());
        executeValidateProcess(webActionDb, listDataMgmt);

    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {
        logger.info("Starting the validate process for exception list, for request {} and action {}",
                listDataMgmt.getRequestMode(), listDataMgmt.getAction());

        // single and add
        try {
            String imsi = listDataMgmt.getImsi();
            String imei = listDataMgmt.getImei();
            String msisdn = listDataMgmt.getMsisdn();
            fileOperations.createDirectory(appConfig.getListMgmtFilePath() + "/" + listDataMgmt.getTransactionId() + "/");
            // check all should be null or empty
            String validateOutput = commonFunctions.validateEntry(imsi, imei, msisdn);

            if (validateOutput.equalsIgnoreCase("")) {
                logger.info("The entry is valid, it will be processed");
            } else {
                File outFile = new File(appConfig.getListMgmtFilePath() + "/" + listDataMgmt.getTransactionId() + "/" + listDataMgmt.getTransactionId() + ".txt");
                PrintWriter writer = new PrintWriter(outFile);
                logger.info("The entry failed the validation, with reason {}", validateOutput);
                writer.println("MSISDN,IMSI,IMEI,Reason"); // print header in file
                writer.println(msisdn + "," + imsi + "," + imei + "," + validateOutput);
                commonFunctions.updateFailStatus(webActionDb, listDataMgmt);
                writer.close();
                return;
            }
            webActionDbRepository.updateWebActionStatus(3, webActionDb.getId());
            executeProcess(webActionDb, listDataMgmt);
        } catch (Exception ex) {
            logger.error("Exception in validating the entry {} with message {}", listDataMgmt, ex.getMessage());
            commonFunctions.updateFailStatus(webActionDb, listDataMgmt);
        }
    }

    public void executeProcess(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {
        try {
            operatorSeriesService.fillOperatorSeriesHash();
            File outFile = new File(appConfig.getListMgmtFilePath() + "/" + listDataMgmt.getTransactionId() + "/" + listDataMgmt.getTransactionId()+ ".txt");
            PrintWriter writer = new PrintWriter(outFile);
            writer.println("MSISDN,IMSI,IMEI,Reason");
            boolean status = commonFunctions.processExceptionSingleAddEntry(listDataMgmt, null, 1, writer);
            writer.close();
            if(status) {
                commonFunctions.updateSuccessStatus(webActionDb, listDataMgmt);
            } else commonFunctions.updateFailStatus(webActionDb, listDataMgmt);
        } catch (Exception ex) {
            logger.error("Error while processing the entry for exception list, for request {} and action {}",
                    listDataMgmt.getRequestType(), listDataMgmt.getAction());
            commonFunctions.updateFailStatus(webActionDb, listDataMgmt);
        }
    }



}

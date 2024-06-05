package com.glocks.web_parser.service.parser.ListMgmt.blackList;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.config.DbConfigService;
import com.glocks.web_parser.constants.FileType;
import com.glocks.web_parser.constants.ListType;
import com.glocks.web_parser.model.app.ListDataMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.SysParamRepository;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.fileCopy.ListFileManagementService;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.operatorSeries.OperatorSeriesService;
import com.glocks.web_parser.service.parser.ListMgmt.CommonFunctions;
import com.glocks.web_parser.service.parser.ListMgmt.utils.BlackListUtils;
import com.glocks.web_parser.validator.Validation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;

@Service
public class BlackSingleDel implements IRequestTypeAction {

    private final Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    WebActionDbRepository webActionDbRepository;
    @Autowired
    Validation validation;
    @Autowired
    AppConfig appConfig;
    @Autowired
    OperatorSeriesService operatorSeriesService;
    @Autowired
    CommonFunctions commonFunctions;
    @Autowired
    ListFileManagementService listFileManagementService;
    @Autowired
    FileOperations fileOperations;
    @Autowired
    SysParamRepository sysParamRepository;
    @Autowired
    DbConfigService dbConfigService;
    @Autowired
    BlackListUtils blackListUtils;


    @Override
    public  void executeInitProcess(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {
        logger.info("Starting the init process for black list, for request {} and action {}",
                listDataMgmt.getRequestMode(), listDataMgmt.getAction());

        webActionDbRepository.updateWebActionStatus(2, webActionDb.getId());
        executeValidateProcess(webActionDb, listDataMgmt);

    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {
        logger.info("Starting the validate process for black list, for request {} and action {}",
                listDataMgmt.getRequestMode(), listDataMgmt.getAction());
        String imsiPrefixValue = sysParamRepository.getValueFromTag("imsiPrefix");
        String msisdnPrefixValue = sysParamRepository.getValueFromTag("msisdnPrefix");
        // single and add
        try {


            String imsi = listDataMgmt.getImsi();
            String imei = listDataMgmt.getImei();
            String msisdn = listDataMgmt.getMsisdn();
            fileOperations.createDirectory(appConfig.getListMgmtFilePath() + "/" + listDataMgmt.getTransactionId() + "/");
            // check all should be null or empty
            String validateOutput = commonFunctions.validateEntry(imsi, imei, msisdn, msisdnPrefixValue.split(",", -1),
                    imsiPrefixValue.split(",", -1));

            if (validateOutput.equalsIgnoreCase("")) {
                logger.info("The entry is valid, it will be processed");
            } else {
                File outFile = new File(appConfig.getListMgmtFilePath() + "/" + listDataMgmt.getTransactionId() + "/" + listDataMgmt.getTransactionId() + ".csv");
                PrintWriter writer = new PrintWriter(outFile);
                logger.info("The entry failed the validation, with reason {}", validateOutput);
                writer.println("MSISDN,IMSI,IMEI,Reason"); // print header in file
                writer.println((msisdn == null ? "":msisdn) + "," + (imsi==null? "":imsi) + "," + (imei==null?"":imei)+","+dbConfigService.getValue(validateOutput));
                commonFunctions.updateFailStatus(webActionDb, listDataMgmt, 1, 0, 1);
                writer.close();
                listFileManagementService.saveListManagementEntity(listDataMgmt.getTransactionId(), ListType.BLACKLIST, FileType.SINGLE,
                        appConfig.getListMgmtFilePath() + "/" + listDataMgmt.getTransactionId() + "/",
                        listDataMgmt.getTransactionId() + ".csv", 1L);
                return;
            }
            webActionDbRepository.updateWebActionStatus(3, webActionDb.getId());
            executeProcess(webActionDb, listDataMgmt);
        }catch (Exception ex) {
            logger.error("Exception in validating the entry {} with message {}", listDataMgmt, ex.getMessage());
            commonFunctions.updateFailStatus(webActionDb, listDataMgmt, 1, 0, 1);
        }
    }

    public void executeProcess(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {
        try {
            operatorSeriesService.fillOperatorSeriesHash();
            File outFile = new File(appConfig.getListMgmtFilePath() + "/" + listDataMgmt.getTransactionId() + "/" + listDataMgmt.getTransactionId()+ ".csv");
            PrintWriter writer = new PrintWriter(outFile);
            writer.println("MSISDN,IMSI,IMEI,Reason");
            boolean status = blackListUtils.processBlackSingleDelEntry(listDataMgmt, null, 1, writer);
            writer.close();
            listFileManagementService.saveListManagementEntity(listDataMgmt.getTransactionId(), ListType.BLACKLIST, FileType.SINGLE,
                    appConfig.getListMgmtFilePath() + "/" + listDataMgmt.getTransactionId() + "/",
                    listDataMgmt.getTransactionId() + ".csv", 1L);
            if(status) {
                commonFunctions.updateSuccessStatus(webActionDb, listDataMgmt, 1, 1, 0);
            } else commonFunctions.updateFailStatus(webActionDb, listDataMgmt, 1, 0, 1);
        } catch (Exception ex) {
            logger.error("Error while processing the entry for black list, for request {} and action {}",
                    listDataMgmt.getRequestType(), listDataMgmt.getAction());
            commonFunctions.updateFailStatus(webActionDb, listDataMgmt, 1, 0, 1);
        }
    }
}

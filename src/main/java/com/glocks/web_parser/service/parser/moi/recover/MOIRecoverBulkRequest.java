package com.glocks.web_parser.service.parser.moi.recover;

import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.StolenDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MOIRecoverBulkRequest implements RequestTypeHandler<StolenDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIService moiService;
    private final WebActionDbRepository webActionDbRepository;
    private final AppConfig appConfig;
    private final FileOperations fileOperations;
    private final MOIRecoverService moiRecoverService;
    private final AlertService alertService;
    Map<String, String> map = new HashMap<>();

    @Override
    public void executeInitProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        executeValidateProcess(webActionDb, stolenDeviceMgmt);
    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        String uploadedFileName = stolenDeviceMgmt.getFileName();
        String transactionId = webActionDb.getTxnId();
        String uploadedFilePath = appConfig.getMoiFilePath() + "/" + transactionId + "/" + uploadedFileName;
        logger.info("Uploaded file path is {}", uploadedFilePath);
        if (!fileOperations.checkFileExists(uploadedFilePath)) {
            logger.error("Uploaded file does not exists in path {} for lost ID {}", uploadedFilePath, transactionId);
            alertService.raiseAnAlert(transactionId, ConfigurableParameter.FILE_MISSING_ALERT.getValue(), webActionDb.getSubFeature(), transactionId, 0);
            return;
        }
        if (!moiService.areHeadersValid(uploadedFilePath, "RECOVER", 1)) {
            moiService.updateStatusAsFailInLostDeviceMgmt(webActionDb, transactionId);
            return;
        }

        map.put("uploadedFileName", uploadedFileName);
        map.put("transactionId", transactionId);
        map.put("uploadedFilePath", uploadedFilePath);
        executeProcess(webActionDb, stolenDeviceMgmt);
    }


    @Override
    public void executeProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        moiRecoverService.fileProcessing(map.get("uploadedFilePath"), stolenDeviceMgmt);
        moiService.updateStatusInLostDeviceMgmt("Done", stolenDeviceMgmt.getLostId());
        logger.info("updated status as Done");
        webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
        logger.info("updated state as Done against {}", webActionDb.getTxnId());
    }

}

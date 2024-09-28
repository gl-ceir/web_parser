package com.glocks.web_parser.service.parser.moi.recover;

import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
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
public class MOIRecoverBulkRequest implements RequestTypeHandler<LostDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIService moiService;
    private final WebActionDbRepository webActionDbRepository;
    private final AppConfig appConfig;
    private final FileOperations fileOperations;
    private final MOIRecoverService moiRecoverService;
    private final AlertService alertService;
    Map<String, String> map = new HashMap<>();

    @Override
    public void executeInitProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        executeValidateProcess(webActionDb, lostDeviceMgmt);
    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        String uploadedFileName = lostDeviceMgmt.getFileName();
        String transactionId = lostDeviceMgmt.getLostId();
        String uploadedFilePath = appConfig.getMoiFilePath() + "/" + transactionId + "/" + uploadedFileName;
        logger.info("Uploaded file path is {}", uploadedFilePath);
        if (!fileOperations.checkFileExists(uploadedFilePath)) {
            logger.error("Uploaded file does not exists in path {} for lost ID {}", uploadedFilePath, transactionId);
            alertService.raiseAnAlert(transactionId, ConfigurableParameter.ALERT_RECOVER.getValue(), "MOI Recover", uploadedFileName + " with transaction id " + transactionId, 0);
            return;
        }
        map.put("uploadedFileName", uploadedFileName);
        map.put("transactionId", transactionId);
        map.put("uploadedFilePath", uploadedFilePath);
        executeProcess(webActionDb, lostDeviceMgmt);
    }


    @Override
    public void executeProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        String transactionId = map.get("transactionId");
        String uploadedFilePath = map.get("uploadedFilePath");
        boolean isFileProcessingPass = moiRecoverService.fileValidation(uploadedFilePath, lostDeviceMgmt);
        if (isFileProcessingPass) {
            moiService.updateStatusInLostDeviceMgmt("Done", transactionId);
            webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        }
    }

}

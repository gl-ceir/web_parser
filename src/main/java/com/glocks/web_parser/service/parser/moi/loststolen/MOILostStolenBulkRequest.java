package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.alert.AlertService;
import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.fileOperations.FileOperations;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
public class MOILostStolenBulkRequest implements RequestTypeHandler<LostDeviceMgmt> {
    private Logger logger = LogManager.getLogger(this.getClass());
    private WebActionDbRepository webActionDbRepository;
    private MOIService moiService;
    private FileOperations fileOperations;
    Map<String, String> map = new HashMap<>();
    private AlertService alertService;
    private MOILostStolenService moiLostStolenService;
    private AppConfig appConfig;

    public MOILostStolenBulkRequest(WebActionDbRepository webActionDbRepository, MOIService moiService, FileOperations fileOperations, AlertService alertService, MOILostStolenService moiLostStolenService, AppConfig appConfig) {
        this.webActionDbRepository = webActionDbRepository;
        this.moiService = moiService;
        this.fileOperations = fileOperations;
        this.alertService = alertService;
        this.moiLostStolenService = moiLostStolenService;
        this.appConfig = appConfig;
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
            alertService.raiseAnAlert(transactionId, ConfigurableParameter.ALERT_LOST_STOLEN.getValue(), "MOI stolen bulk", uploadedFileName + " with transaction id " + transactionId, 0);
            return;
        }
        if (!moiService.areHeadersValid(uploadedFilePath, "STOLEN", 9)) {
            moiService.updateStatusAsFailInLostDeviceMgmt(webActionDb,transactionId);
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
        String transactionId = map.get("transactionId");
        String processedFilePath = map.get("moiFilePath") + "/" + transactionId + "/" + transactionId + ".csv";
        logger.info("Processed file path is {}", processedFilePath);
        moiLostStolenService.fileProcess(webActionDb, lostDeviceMgmt, map.get("uploadedFileName"), map.get("uploadedFilePath"), Integer.parseInt(moiService.greyListDuration()));
        moiService.updateStatusInLostDeviceMgmt("DONE", lostDeviceMgmt.getRequestId());
        webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
    }
}
package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.StolenDeviceDetail;
import com.glocks.web_parser.model.app.StolenDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.parser.moi.pendingverification.NotificationForPendingVerification;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MOILostStolenService {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final AppConfig appConfig;
    private final MOIService moiService;
    private final StolenDeviceDetailRepository stolenDeviceDetailRepository;
    private final NotificationForPendingVerification notificationForPendingVerification;

    public void fileProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt, String uploadedFileName, String uploadedFilePath, int greyListDuration) {
        try (BufferedReader reader = new BufferedReader(new FileReader(uploadedFilePath))) {
            String record;
            //  String[] header;
            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
            String[] split;
            boolean headerSkipped = false;
            while ((record = reader.readLine()) != null) {
                if (!record.trim().isEmpty()) {
                    if (!headerSkipped) {
                        // header = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        headerSkipped = true;
                    } else {
                        split = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        imeiSeriesModel.setImeiSeries(split, "STOLEN");
                        logger.info("IMEISeriesModel {}", imeiSeriesModel);
                        List<String> imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
                        if (!imeiList.isEmpty()) imeiList.forEach(imei -> {
                            if (!moiService.isNumericAndValid(imei)) {
                                logger.info("Invalid IMEI found");
                            } else {
                                this.recordProcess(imei, stolenDeviceMgmt, stolenDeviceMgmt.getDeviceLostDateTime(), "Bulk", greyListDuration);
                            }
                        });
                    }
                }
            }
            if (Objects.nonNull(stolenDeviceMgmt.getDeviceOwnerNationality())) {
                String channel = stolenDeviceMgmt.getDeviceOwnerNationality().equals("0") ? "SMS" : "EMAIL";
                notificationForPendingVerification.sendNotification(webActionDb, stolenDeviceMgmt, channel, uploadedFilePath, ConfigurableParameter.MOI_VERIFICATION_DONE_MSG.getValue());
                logger.info("notification sent to {} mode user , 0:Cambodian 1:Non-cambodian", stolenDeviceMgmt.getDeviceOwnerNationality());
            }
        } catch (Exception exception) {
            logger.error("Exception in processing the file " + exception.getMessage());
        }
    }

    public void recordProcess(String imei, StolenDeviceMgmt stolenDeviceMgmt, String deviceLostDateTime, String mode, int greyListDuration) {
        if (greyListDuration == 0) moiService.greyListDurationIsZero(imei, mode, stolenDeviceMgmt);
        else if (greyListDuration > 0)
            moiService.greyListDurationGreaterThanZero(greyListDuration, imei, mode, stolenDeviceMgmt);
        else logger.info("GREY_LIST_DURATION tag invalid value {}", greyListDuration);

        if (moiService.isDateFormatValid(deviceLostDateTime)) {
            moiService.imeiPairDetail(deviceLostDateTime, mode);
        }
        lostDeviceDetailAction(imei, stolenDeviceMgmt, mode);
    }

    public void lostDeviceDetailAction(String imei, StolenDeviceMgmt stolenDeviceMgmt, String mode) {
        if (mode.equalsIgnoreCase("SINGLE")) {
            StolenDeviceDetail stolenDeviceDetail = StolenDeviceDetail.builder().imei(imei).deviceModel(stolenDeviceMgmt.getDeviceModel()).deviceBrand(stolenDeviceMgmt.getDeviceBrand()).contactNumber(stolenDeviceMgmt.getContactNumber()).requestId(stolenDeviceMgmt.getRequestId()).status("Done").requestType("Stolen").build();
            logger.info("stolenDeviceDetail {}", stolenDeviceDetail);
            StolenDeviceDetail save = moiService.save(stolenDeviceDetail, stolenDeviceDetailRepository::save);
            if (save != null) {
                logger.info("Record inserted for imei {} in stolen_device_detail", imei);
            } else {
                logger.info("Failed to insert record for imei {} in stolen_device_detail", imei);
            }
        }
        if (mode.equalsIgnoreCase("BULK")) {
            if (stolenDeviceDetailRepository.updateStatus("Done", imei) > 0) {
                logger.info("Record updated for imei {} in stolen_device_detail", imei);
            } else {
                logger.info("Failed to update record for imei {} in stolen_device_detail", imei);
            }
        }
    }
}
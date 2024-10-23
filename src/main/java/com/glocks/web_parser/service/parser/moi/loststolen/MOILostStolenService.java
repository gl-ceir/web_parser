package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceDetail;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.parser.moi.pendingverification.NotificationForPendingVerification;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MOILostStolenService {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final AppConfig appConfig;
    private final MDRRepository mdrRepository;
    private final MOIService moiService;
    private final LostDeviceMgmtRepository lostDeviceMgmtRepository;
    private final LostDeviceDetailRepository lostDeviceDetailRepository;
    private final EirsInvalidImeiRepository eirsInvalidImeiRepository;
    private final DuplicateDeviceDetailRepository duplicateDeviceDetailRepository;
    private final WebActionDbRepository webActionDbRepository;
    private final NotificationForPendingVerification notificationForPendingVerification;
    static int successCount, failCount, i = 0;
    List<LostDeviceDetail> lostDeviceDetailPayload = new LinkedList<>();


    public void fileProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt, String uploadedFileName, String uploadedFilePath, int greyListDuration) {
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
                                this.recordProcess(imei, lostDeviceMgmt, lostDeviceMgmt.getDeviceLostDateTime(), "Bulk", greyListDuration);
                            }
                        });
                    }
                }
            }
            if (Objects.nonNull(lostDeviceMgmt.getDeviceOwnerNationality())) {
                String channel = lostDeviceMgmt.getDeviceOwnerNationality().equals("0") ? "SMS" : "EMAIL";
                notificationForPendingVerification.sendNotification(webActionDb, lostDeviceMgmt, channel, uploadedFilePath);
                logger.info("notification sent to {} mode user , 0:Cambodian 1:Non-cambodian", lostDeviceMgmt.getDeviceOwnerNationality());
            }
        } catch (Exception exception) {
            logger.error("Exception in processing the file " + exception.getMessage());
        }
    }

    public void recordProcess(String imei, LostDeviceMgmt lostDeviceMgmt, String deviceLostDateTime, String mode, int greyListDuration) {
        if (greyListDuration == 0) moiService.greyListDurationIsZero(imei, mode, lostDeviceMgmt);
        else if (greyListDuration > 0)
            moiService.greyListDurationGreaterThanZero(greyListDuration, imei, mode, lostDeviceMgmt);
        else logger.info("GREY_LIST_DURATION tag invalid value {}", greyListDuration);

        if (moiService.isDateFormatValid(deviceLostDateTime)) {
            moiService.imeiPairDetail(deviceLostDateTime, mode);
        }
        lostDeviceDetailAction(imei, lostDeviceMgmt, mode);
    }

    public void lostDeviceDetailAction(String imei, LostDeviceMgmt lostDeviceMgmt, String mode) {
        if (mode.equalsIgnoreCase("SINGLE")) {
            LostDeviceDetail lostDeviceDetail = LostDeviceDetail.builder().imei(imei).deviceModel(lostDeviceMgmt.getDeviceModel()).deviceBrand(lostDeviceMgmt.getDeviceBrand()).contactNumber(lostDeviceMgmt.getContactNumber()).requestId(lostDeviceMgmt.getRequestId()).status("Done").requestType("Stolen").build();
            logger.info("lostDeviceDetail {}", lostDeviceDetail);
            LostDeviceDetail save = moiService.save(lostDeviceDetail, lostDeviceDetailRepository::save);
            if (save != null) {
                logger.info("Record inserted for imei {} in lost_device_detail", imei);
            } else {
                logger.info("Failed to insert record for imei {} in lost_device_detail", imei);
            }
        }
        if (mode.equalsIgnoreCase("BULK")) {
            if (lostDeviceDetailRepository.updateStatus("Done", imei) > 0) {
                logger.info("Record updated for imei {} in lost_device_detail", imei);
            } else {
                logger.info("Failed to update record for imei {} in lost_device_detail", imei);
            }
        }
    }
}
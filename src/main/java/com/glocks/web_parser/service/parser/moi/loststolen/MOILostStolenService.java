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
            String[] header;
            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
            String[] split;
            boolean headerSkipped = false;
            while ((record = reader.readLine()) != null) {
                if (!record.trim().isEmpty()) {
                    if (!headerSkipped) {
                        header = record.split(appConfig.getListMgmtFileSeparator(), -1);
//printWriter.println(moiService.joiner(header, ""));
                        headerSkipped = true;
                    } else {
                        split = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        imeiSeriesModel.setImeiSeries(split, "STOLEN");
                        logger.info("IMEISeriesModel {}", imeiSeriesModel);
                        List<String> imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
                        if (!imeiList.isEmpty()) imeiList.forEach(imei -> {
                            if (!moiService.isNumericAndValid(imei)) {
//    printWriter.println(moiService.joiner(split, ",Invalid Format"));
                                logger.info("Invalid IMEI found");
                            } else {
                                this.recordProcess(imei, lostDeviceMgmt, lostDeviceMgmt.getDeviceLostDateTime(), "BULK", greyListDuration);
                            }
                        });

/*        if (!imeiList.isEmpty()) imeiList.forEach(imei -> {
if (greyListDuration == 0)
moiService.greyListDurationIsZero(imei, "Single", lostDeviceMgmt);
else if (greyListDuration > 0)
moiService.greyListDurationGreaterThanZero(greyListDuration, imei, "Single", lostDeviceMgmt);
else logger.info("pass a valid 'GREY_LIST_DURATION' tag value");
});
moiService.imeiPairDetail(deviceLostDateTime);
moiService.updateStatusInLostDeviceMgmt("DONE", lostDeviceMgmt.getRequestId());
webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
*/

                    }
                }
            }
            if (Objects.nonNull(lostDeviceMgmt.getDeviceOwnerNationality())) {
                notificationForPendingVerification.sendNotification(webActionDb, lostDeviceMgmt, lostDeviceMgmt.getDeviceOwnerNationality(), uploadedFilePath);
                logger.info("notification sent via {} mode to user , 0:citiizen 1:Non combodian", lostDeviceMgmt.getDeviceOwnerNationality());
            }
//  printWriter.close();
        } catch (Exception ex) {
            logger.error("Exception in processing the file " + ex.getMessage());
        }
    }

    public void recordProcess(String imei, LostDeviceMgmt lostDeviceMgmt, String deviceLostDateTime, String mode, int greyListDuration) {
        if (greyListDuration == 0) moiService.greyListDurationIsZero(imei, mode, lostDeviceMgmt);
        else if (greyListDuration > 0)
            moiService.greyListDurationGreaterThanZero(greyListDuration, imei, mode, lostDeviceMgmt);
        else logger.info("pass a valid 'GREY_LIST_DURATION' tag value");

        if (moiService.isDateFormatValid(deviceLostDateTime)) {
            moiService.imeiPairDetail(deviceLostDateTime, mode);
        }
    }
}
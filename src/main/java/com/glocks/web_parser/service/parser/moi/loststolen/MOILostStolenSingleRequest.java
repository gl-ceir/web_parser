package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class MOILostStolenSingleRequest implements RequestTypeHandler<LostDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final WebActionDbRepository webActionDbRepository;
    private final MOIService moiService;
    private final BlackListRepository blackListRepository;
    private final ImeiPairDetailRepository imeiPairDetailRepository;
    private final ImeiPairDetailHisRepository imeiPairDetailHisRepository;
    private final BlackListHisRepository blackListHisRepository;
    private final GreyListHisRepository greyListHisRepository;
    private final GreyListRepository greyListRepository;
    private final MOILostStolenService moiLostStolenService;
    static int greyListDuration;

    @Override
    public void executeInitProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        executeValidateProcess(webActionDb, lostDeviceMgmt);
    }


    @Override
    public void executeValidateProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        try {
            greyListDuration = Integer.parseInt(moiService.greyListDuration());
            executeProcess(webActionDb, lostDeviceMgmt);
        } catch (NumberFormatException e) {
            logger.info("Invalid GREY_LIST_DURATION value");
        }
    }

    @Override
    public void executeProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        String deviceLostDateTime = lostDeviceMgmt.getDeviceLostDateTime();
        logger.info("GREY_LIST_DURATION : {}  deviceLostDateTime {} ", greyListDuration, deviceLostDateTime);
        if (Objects.nonNull(deviceLostDateTime)) {
            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
            BeanUtils.copyProperties(lostDeviceMgmt, imeiSeriesModel);
            logger.info("IMEISeriesModel {}", imeiSeriesModel);
            List<String> imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
            if (!imeiList.isEmpty()) imeiList.forEach(imei -> {
                if (!moiService.isNumericAndValid(imei)) {
                    logger.info("Invalid IMEI {} found", imei);
                } else {
                    moiLostStolenService.recordProcess(imei, lostDeviceMgmt, deviceLostDateTime, "Single", greyListDuration);
                }
            });
//            as per sharad userstatus should be marked as blocked
            moiService.updateStatusInLostDeviceMgmt("Done", lostDeviceMgmt.getRequestId());
            logger.info("updated status as Done");
            webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
            logger.info("updated state as Done against {}", webActionDb.getTxnId());
        } else {
            logger.info("Invalid deviceLostDateTime value {}", deviceLostDateTime);
        }
    }
}
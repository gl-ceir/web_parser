package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.model.app.StolenDeviceMgmt;
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

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MOILostStolenSingleRequest implements RequestTypeHandler<StolenDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final WebActionDbRepository webActionDbRepository;
    private final MOIService moiService;
    private final MOILostStolenService moiLostStolenService;
    static int greyListDuration;

    @Override
    public void executeInitProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        executeValidateProcess(webActionDb, stolenDeviceMgmt);
    }


    @Override
    public void executeValidateProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        try {
            greyListDuration = Integer.parseInt(moiService.greyListDuration());
            executeProcess(webActionDb, stolenDeviceMgmt);
        } catch (NumberFormatException e) {
            logger.info("Invalid GREY_LIST_DURATION value");
        }
    }

    @Override
    public void executeProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        String deviceLostDateTime = stolenDeviceMgmt.getDeviceLostDateTime();
        logger.info("GREY_LIST_DURATION : {}  deviceLostDateTime {} ", greyListDuration, deviceLostDateTime);
        if (Objects.nonNull(deviceLostDateTime)) {
            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
            BeanUtils.copyProperties(stolenDeviceMgmt, imeiSeriesModel);
            logger.info("IMEISeriesModel {}", imeiSeriesModel);
            List<String> imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
            if (!imeiList.isEmpty()) imeiList.forEach(imei -> {
                if (!moiService.isNumericAndValid(imei)) {
                    logger.info("Invalid IMEI {} found", imei);
                } else {
                    moiLostStolenService.recordProcess(imei, stolenDeviceMgmt, deviceLostDateTime, "Single", greyListDuration);
                }
            });
            moiService.updateStatusInLostDeviceMgmt("Done", stolenDeviceMgmt.getRequestId());
            logger.info("updated status as Done");
            webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
            logger.info("updated state as Done against {}", webActionDb.getTxnId());
        } else {
            logger.info("Invalid deviceLostDateTime value {}", deviceLostDateTime);
        }
    }
}
package com.glocks.web_parser.service.parser.moi.recover;

import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MOIRecoverSingleRequest implements RequestTypeHandler<LostDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIService moiService;
    private final WebActionDbRepository webActionDbRepository;
    private final MOIRecoverService moiRecoverService;

    List<String> imeiList = new ArrayList<>();

    @Override
    public void executeInitProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        executeValidateProcess(webActionDb, lostDeviceMgmt);
    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
        BeanUtils.copyProperties(lostDeviceMgmt, imeiSeriesModel);
        imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
        if (!imeiList.isEmpty()) {
            logger.info("No IMEI found for txn id {}", webActionDb.getTxnId());
            return;
        }
        executeProcess(webActionDb, lostDeviceMgmt);
    }

    @Override
    public void executeProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        moiRecoverService.actionAtRecord(lostDeviceMgmt, imeiList);
        moiService.updateStatusInLostDeviceMgmt("DONE", lostDeviceMgmt.getLostId());
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
 /*       try {
            String lostId = lostDeviceMgmt.getLostId();
            imeiList.forEach(imei -> {
                moiRecoverService.blackListFlow(imei);
                moiRecoverService.greyListFlow(imei);
                moiRecoverService.lostDeviceDetailFlow(imei, lostDeviceMgmt);
            });
            moiService.updateStatusInLostDeviceMgmt("DONE", lostId);
            webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        } catch (Exception e) {
            logger.info("Oops something break while running recover single request", e.getMessage());
        }*/
    }
}
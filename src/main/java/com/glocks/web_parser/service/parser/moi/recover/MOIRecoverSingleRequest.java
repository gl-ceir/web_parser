package com.glocks.web_parser.service.parser.moi.recover;

import com.glocks.web_parser.model.app.StolenDeviceMgmt;
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
public class MOIRecoverSingleRequest implements RequestTypeHandler<StolenDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIService moiService;
    private final WebActionDbRepository webActionDbRepository;
    private final MOIRecoverService moiRecoverService;

    List<String> imeiList = new ArrayList<>();

    @Override
    public void executeInitProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        executeValidateProcess(webActionDb, stolenDeviceMgmt);
    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
        BeanUtils.copyProperties(stolenDeviceMgmt, imeiSeriesModel);
        imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
        if (imeiList.isEmpty()) {
            logger.info("No IMEI found for txn id {}", webActionDb.getTxnId());
            return;
        }
        executeProcess(webActionDb, stolenDeviceMgmt);
    }

    @Override
    public void executeProcess(WebActionDb webActionDb, StolenDeviceMgmt stolenDeviceMgmt) {
        moiRecoverService.actionAtRecord(stolenDeviceMgmt, imeiList,"Single");
        moiService.updateStatusInLostDeviceMgmt("Done", stolenDeviceMgmt.getRequestId());
        moiService.webActionDbOperation(4, webActionDb.getId());

    }
}
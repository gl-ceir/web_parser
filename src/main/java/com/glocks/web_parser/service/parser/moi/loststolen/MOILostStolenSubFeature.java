package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MOILostStolenSubFeature {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOILostStolenSingleRequest moiLostStolenSingleRequest;
    private final MOILostStolenBulkRequest moiLostStolenBulkRequest;

    public void delegateInitRequest(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        RequestTypeHandler requestTypeHandler = checkType(lostDeviceMgmt);
        if (requestTypeHandler != null) {
            logger.info("executed {} operation", lostDeviceMgmt.getRequestMode());
            requestTypeHandler.executeInitProcess(webActionDb, lostDeviceMgmt);
        } else logger.info("Invalid request mode");
    }

    public RequestTypeHandler checkType(LostDeviceMgmt lostDeviceMgmt) {
        String requestType = lostDeviceMgmt.getRequestMode();
        RequestTypeHandler requestTypeSelection = requestType.equalsIgnoreCase(ConfigurableParameter.SINGLE.getValue()) ? moiLostStolenSingleRequest : requestType.equalsIgnoreCase(ConfigurableParameter.BULK.getValue()) ? moiLostStolenBulkRequest : null;
        //RequestTypeHandler requestTypeSelection = requestType.equalsIgnoreCase(ConfigurableParameter.SINGLE.getValue()) ? moiLostStolenSingleRequest : null;
        return requestTypeSelection;
    }
}
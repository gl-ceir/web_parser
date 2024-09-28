package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
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

    public void delegateInitRequest(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        RequestTypeHandler requestTypeHandler = checkType(lostDeviceMgmt);
        requestTypeHandler.executeInitProcess(webActionDb, lostDeviceMgmt);
    }

    public RequestTypeHandler checkType(LostDeviceMgmt lostDeviceMgmt) {
        String requestType = lostDeviceMgmt.getRequestMode();
        //RequestTypeHandler requestTypeSelection = requestType.equalsIgnoreCase(RequestTypeHandler.SINGLE) ? moiLostStolenSingleRequest : requestType.equalsIgnoreCase(RequestTypeHandler.BULK) ? null : null;
        RequestTypeHandler requestTypeSelection = moiLostStolenSingleRequest;
        logger.info("executed {} operation", requestType);
        return requestTypeSelection;
    }
}
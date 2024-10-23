package com.glocks.web_parser.service.parser.moi.recover;

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
public class MOIRecoverSubFeature {

    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIRecoverSingleRequest moiRecoverSingleRequest;
    private final MOIRecoverBulkRequest moiRecoverBulkRequest;

    public void delegateInitRequest(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        logger.info("LostDeviceMgmt response based on request ID {} : {}", lostDeviceMgmt.getRequestId(), lostDeviceMgmt);

        // logger.info("LostDeviceMgmt response based on request ID {} : {}", lostDeviceMgmt.getLostId(), lostDeviceMgmt);
        RequestTypeHandler requestTypeHandler = checkType(lostDeviceMgmt);
        if (requestTypeHandler != null)
            requestTypeHandler.executeInitProcess(webActionDb, lostDeviceMgmt);
        else logger.info("Invalid request mode");
    }

    public RequestTypeHandler checkType(LostDeviceMgmt lostDeviceMgmt) {
        String requestType = lostDeviceMgmt.getRequestMode();
        RequestTypeHandler requestTypeSelection = requestType.equalsIgnoreCase(ConfigurableParameter.SINGLE.getValue()) ? moiRecoverSingleRequest : requestType.equalsIgnoreCase(ConfigurableParameter.BULK.getValue()) ? moiRecoverBulkRequest : null;
        logger.info("executed {} operation", requestType);
        return requestTypeSelection;
    }

/*
public void delegateValidateRequest(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
RequestTypeHandler requestTypeAction = checkType(searchImeiByPoliceMgmt);
requestTypeAction.executeValidateProcess(webActionDb, searchImeiByPoliceMgmt);
}

public void delegateExecuteProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
RequestTypeHandler requestTypeAction = checkType(searchImeiByPoliceMgmt);
requestTypeAction.executeProcess(webActionDb, searchImeiByPoliceMgmt);
}
*/
}
package com.glocks.web_parser.service.parser.moi.pendingverification;

import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingVerificationFeature {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final PendingVerificationRequest pendingVerificationRequest;

    public void delegateInitRequest(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        RequestTypeHandler requestTypeHandler = checkType(lostDeviceMgmt);
        requestTypeHandler.executeInitProcess(webActionDb, lostDeviceMgmt);

    }

    public RequestTypeHandler checkType(LostDeviceMgmt lostDeviceMgmt) {
        RequestTypeHandler requestTypeSelection = pendingVerificationRequest;
        return requestTypeSelection;
    }
}

package com.glocks.web_parser.service.parser.MOI.IMEI_SEARCH_RECOVERY;

import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.service.parser.MOI.common.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IMEISearchRecoverySubFeature {

    private final Logger logger = LogManager.getLogger(this.getClass());
    private final IMEISearchRecoverySingleRequest imeiSearchRecoverySingleRequest;
    private final IMEISearchRecoveryBulkRequest imeiSearchRecoveryBulkRequest;

    public void delegateInitRequest(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        logger.info("SearchImeiByPoliceMgmt response based on Txn ID {} : {}", searchImeiByPoliceMgmt.getTransactionId(), searchImeiByPoliceMgmt);
        RequestTypeHandler requestTypeHandler = checkType(searchImeiByPoliceMgmt);
        requestTypeHandler.executeInitProcess(webActionDb, searchImeiByPoliceMgmt);
    }

    public RequestTypeHandler checkType(SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        String requestType = searchImeiByPoliceMgmt.getRequestMode();
        RequestTypeHandler requestTypeSelection = requestType.equalsIgnoreCase(RequestTypeHandler.SINGLE) ? imeiSearchRecoverySingleRequest : requestType.equalsIgnoreCase(RequestTypeHandler.BULK) ? imeiSearchRecoveryBulkRequest : null;
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
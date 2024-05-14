package com.glocks.web_parser.service.parser.ListMgmt.exceptionList;

import com.glocks.web_parser.model.app.ListDataMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.glocks.web_parser.constants.ListMgmtConstants.*;

@Service
public class ExceptionListSubFeature {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ExceptionSingleDel exceptionSingleDel;
    @Autowired
    ExceptionSingleAdd exceptionSingleAdd;
    @Autowired
    ExceptionBulkAdd exceptionBulkAdd;
    @Autowired
    ExceptionBulkDel exceptionBulkDel;

    public void delegateInitRequest(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {

        IRequestTypeAction requestTypeAction = checkType(listDataMgmt);
        requestTypeAction.executeInitProcess(webActionDb, listDataMgmt);
    }
    public void delegateValidateRequest(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {
        IRequestTypeAction requestTypeAction = checkType(listDataMgmt);
        requestTypeAction.executeValidateProcess(webActionDb, listDataMgmt);
    }
    public void delegateExecuteProcess(WebActionDb webActionDb, ListDataMgmt listDataMgmt) {
            IRequestTypeAction requestTypeAction = checkType(listDataMgmt);
            requestTypeAction.executeProcess(webActionDb, listDataMgmt);
    }

    public IRequestTypeAction checkType(ListDataMgmt listDataMgmt) {
        String requestType = listDataMgmt.getRequestMode();
        String action = listDataMgmt.getAction();
        if(requestType.equalsIgnoreCase(listMgmtSingleRequestMode) && action.equalsIgnoreCase(listMgmtActionAdd)) {
            return exceptionSingleAdd;
        }
        else if(requestType.equalsIgnoreCase(listMgmtSingleRequestMode) && action.equalsIgnoreCase(listMgmtActionDel)) {
            return exceptionSingleDel;
        }
        else if(requestType.equalsIgnoreCase(listMgmtBulkRequestMode) && action.equalsIgnoreCase(listMgmtActionAdd)) {
            return exceptionBulkAdd;

        }
        else if(requestType.equalsIgnoreCase(listMgmtBulkRequestMode) && action.equalsIgnoreCase(listMgmtActionDel)) {
            return exceptionBulkDel;
        }
        else return null;
    }

}

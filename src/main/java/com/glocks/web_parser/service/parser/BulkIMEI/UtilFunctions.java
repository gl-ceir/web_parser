package com.glocks.web_parser.service.parser.BulkIMEI;

import com.glocks.web_parser.model.app.BulkCheckImeiMgmt;
import com.glocks.web_parser.model.app.EirsResponseParam;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.BulkCheckImeiMgmtRepository;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class UtilFunctions {

    @Autowired
    WebActionDbRepository webActionDbRepository;
    @Autowired
    BulkCheckImeiMgmtRepository bulkCheckImeiMgmtRepository;

    public void updateFailStatus(WebActionDb webActionDb, BulkCheckImeiMgmt bulkCheckImeiMgmt) {
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        bulkCheckImeiMgmtRepository.updateBulkCheckImeiMgmtStatus("FAIL", LocalDateTime.now(), bulkCheckImeiMgmt.getId());
//        alertService.raiseAnAlert(alertId, type, fileName, 0);
    }

    public void updateFailStatus(WebActionDb webActionDb, BulkCheckImeiMgmt bulkCheckImeiMgmt, long totalCount,
                                 long successCount, long failureCount) {
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
        bulkCheckImeiMgmtRepository.updateBulkCheckImeiMgmtStatus("FAIL", LocalDateTime.now(), bulkCheckImeiMgmt.getId(),
                totalCount, successCount, failureCount);
//        alertService.raiseAnAlert(alertId, type, fileName, 0);
    }

    public void updateSuccessStatus(WebActionDb webActionDb, BulkCheckImeiMgmt bulkCheckImeiMgmt) {
        webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
        bulkCheckImeiMgmtRepository.updateBulkCheckImeiMgmtStatus("DONE", LocalDateTime.now(), bulkCheckImeiMgmt.getId());
    }

    public void updateSuccessStatus(WebActionDb webActionDb, BulkCheckImeiMgmt bulkCheckImeiMgmt, long totalCount,
                                    long successCount, long failureCount) {
        webActionDbRepository.updateWebActionStatus(4, webActionDb.getId());
        bulkCheckImeiMgmtRepository.updateBulkCheckImeiMgmtStatus("DONE", LocalDateTime.now(),
                bulkCheckImeiMgmt.getId(), totalCount, successCount, failureCount);
    }

    public String replaceString(String message, long count, String transactionId, String fileName) {
        message = message.replace("<limit>", String.valueOf(count));
        message = message.replace("<txn_id>", transactionId);
        message = message.replace("<file_name>", fileName);
        return message;
    }


    //    MOI feature
    public EirsResponseParam replaceParameter(EirsResponseParam message, String requestId, String contactNumber, String channel) {
        if (channel.equalsIgnoreCase("EMAIL"))
            message.setDescription(message.getDescription().replace("<requestId>", requestId));

        if (channel.equalsIgnoreCase("SMS")) {
            message.setValue(message.getValue().replace("<requestId>", requestId));
            message.setValue(message.getValue().replace("<contactNumber>", contactNumber));
        }
        return message;
    }

}

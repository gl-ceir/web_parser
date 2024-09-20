package com.glocks.web_parser.service.parser.MOI.IMEI_SEARCH_RECOVERY;

import com.glocks.web_parser.model.app.LostDeviceDetail;
import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.parser.MOI.common.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IMEISearchRecoverySingleRequest implements RequestTypeHandler<SearchImeiByPoliceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final ImeiSearchRecoveryService imeiSearchRecoveryService;
    private final WebActionDbRepository webActionDbRepository;

    @Override
    public void executeInitProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        executeProcess(webActionDb, searchImeiByPoliceMgmt);
    }

    @Override
    public void executeValidateProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {

    }

    @Override
    public void executeProcess(WebActionDb webActionDb, SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        String transactionId = searchImeiByPoliceMgmt.getTransactionId();
        String imei1 = searchImeiByPoliceMgmt.getImei1();
        boolean multipleIMEIExist = imeiSearchRecoveryService.isMultipleIMEIExist(searchImeiByPoliceMgmt);
        //for single IMEI
        if (!multipleIMEIExist) {
            Optional<LostDeviceDetail> byImeiAndStatusAndRequestType = imeiSearchRecoveryService.findByImeiAndStatusAndRequestType(imei1, transactionId);
            if (byImeiAndStatusAndRequestType.isPresent()) {
                logger.info("IMEI {} is going to save in search_imei_detail_by_police", imei1);
                imeiSearchRecoveryService.copyRecordLostDeviceMgmtToSearchIMEIDetailByPolice(byImeiAndStatusAndRequestType.get(), transactionId, webActionDb);
                imeiSearchRecoveryService.updateStatus("Done", transactionId);
                webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
            }
        }
        //for Multiple IMEI's
        else {
            List<String> list = imeiSearchRecoveryService.tacList(searchImeiByPoliceMgmt);
            if (!list.isEmpty()) {
                boolean isBrandAndModelValid = imeiSearchRecoveryService.isBrandAndModelValid(list);
                logger.info("Are brand name and model name valid : {} ", isBrandAndModelValid);
                if (!isBrandAndModelValid) {
                    imeiSearchRecoveryService.updateStatusAndCountFoundInLost("Fail", 0, transactionId);
                    webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
                    return;
                } else {
                    List<String> imeiList = imeiSearchRecoveryService.imeiList(searchImeiByPoliceMgmt);
                    imeiList.forEach(x -> {
                        Optional<LostDeviceDetail> byImeiAndStatusAndRequestType = imeiSearchRecoveryService.findByImeiAndStatusAndRequestType(x, transactionId);
                        if (byImeiAndStatusAndRequestType.isPresent()) {
                            logger.info("IMEI {} is going to save in search_imei_detail_by_police", x);
                            imeiSearchRecoveryService.copyRecordLostDeviceMgmtToSearchIMEIDetailByPolice(byImeiAndStatusAndRequestType.get(), transactionId, webActionDb);
                        }
                    });

                }
            }
        }

    }
}
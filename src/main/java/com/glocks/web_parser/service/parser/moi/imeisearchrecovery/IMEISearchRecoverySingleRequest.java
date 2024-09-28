package com.glocks.web_parser.service.parser.moi.imeisearchrecovery;

import com.glocks.web_parser.model.app.LostDeviceDetail;
import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IMEISearchRecoverySingleRequest implements RequestTypeHandler<SearchImeiByPoliceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final MOIService moiService;
    private final IMEISearchRecoveryService imeiSearchRecoveryService;
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
        IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
        BeanUtils.copyProperties(searchImeiByPoliceMgmt, imeiSeriesModel);
        String transactionId = searchImeiByPoliceMgmt.getTransactionId();
        boolean multipleIMEIExist = moiService.isMultipleIMEIExist(imeiSeriesModel);
        if (multipleIMEIExist) {
            if (!imeiSearchRecoveryService.isBrandAndModelGenuine(webActionDb, imeiSeriesModel, transactionId))
                return;
        }
        boolean isLostDeviceDetailExist = false;
        List<String> imeiList = moiService.imeiList(imeiSeriesModel);
        if (!imeiList.isEmpty()) {
            try {
                for (String imei : imeiList) {
                    Optional<LostDeviceDetail> LostDeviceDetailOptional = moiService.findByImeiAndStatusAndRequestType(imei);
                    if (LostDeviceDetailOptional.isPresent()) {
                        boolean isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice = imeiSearchRecoveryService.isRequestIdFound(imei, imeiSeriesModel.getMap().get(imei), webActionDb, transactionId, LostDeviceDetailOptional.get().getRequestId(), "SINGLE", 1);
                        if (isCopiedRecordLostDeviceMgmtToSearchIMEIDetailByPolice) {
                            isLostDeviceDetailExist = true;
                            break;
                        }
                    }
                }

                if (!isLostDeviceDetailExist) {
                    imeiSearchRecoveryService.isLostDeviceDetailEmpty(webActionDb, transactionId);
                }
            } catch (Exception e) {
                moiService.updateStatusAndCountFoundInLost("Fail", 0, transactionId, "Please try after some time");
                webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
                logger.info("Oops!, error occur while execution {}", e.getMessage());
            }
        }
    }
}
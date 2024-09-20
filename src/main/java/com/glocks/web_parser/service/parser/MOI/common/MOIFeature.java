package com.glocks.web_parser.service.parser.MOI.common;

import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.parser.FeatureInterface;
import com.glocks.web_parser.service.parser.MOI.IMEI_SEARCH_RECOVERY.ImeiSearchRecoveryService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;


@Component
@RequiredArgsConstructor
public class MOIFeature implements FeatureInterface {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final ImeiSearchRecoveryService imeiSearchRecoveryService;
    private final WebActionDbRepository webActionDbRepository;

    public void action(WebActionDb wb, String subFeature) {
        String txnId = wb.getTxnId();
        logger.info("Txn ID [{}]", txnId);
        BiConsumer<WebActionDb, Object> webActionDbBiConsumer = FeatureExist.task(subFeature);
        if (Objects.nonNull(webActionDbBiConsumer)) {
            Optional<SearchImeiByPoliceMgmt> optional = imeiSearchRecoveryService.findByTxnId(txnId);
            if (optional.isEmpty()) {
                logger.info("No Txn ID found in SearchImeiByPoliceMgmt");
            } else {
                webActionDbBiConsumer.accept(wb, optional.get());
            }
        }
    }

    @Override
    public void executeInit(WebActionDb wb) {
        String subFeatureName = wb.getSubFeature().trim();
        logger.info("Initialization started for MOI sub feaure : {}", subFeatureName);
        String subFeature = new StringBuilder(subFeatureName).append("_INIT").toString();
        this.action(wb, subFeature);
    }

    @Override
    public void executeProcess(WebActionDb wb) {
        String subFeature = new StringBuilder(wb.getSubFeature()).append("_EXECUTE").toString();
        this.action(wb, subFeature);
    }

    @Override
    public void validateProcess(WebActionDb wb) {
        String subFeature = new StringBuilder(wb.getSubFeature()).append("_VALIDATE").toString();
        this.action(wb, subFeature);
    }
}

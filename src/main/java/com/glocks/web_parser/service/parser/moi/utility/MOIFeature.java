package com.glocks.web_parser.service.parser.moi.utility;

import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.SearchImeiByPoliceMgmtRepository;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.parser.FeatureInterface;
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
    private final WebActionDbRepository webActionDbRepository;
    private final MOIService moiService;
    private final SearchImeiByPoliceMgmtRepository searchImeiByPoliceMgmtRepository;

    public void action(WebActionDb wb, String subFeature) {
        String txnId = wb.getTxnId();
        logger.info("Txn ID [{}]", txnId);
        BiConsumer<WebActionDb, Object> consumer = MOIFeatureExist.task(subFeature);
        if (Objects.nonNull(consumer)) {
            Optional<?> optional = Optional.empty();
            switch (subFeature) {
                case "IMEI_SEARCH_RECOVERY" -> optional = moiService.findByTxnId(txnId);
                case "STOLEN", "LOST", "LOST/STOLEN", "PENDING_VERIFICATION" ->
                        optional = moiService.findByRequestId(txnId);
                case "RECOVER" -> optional = moiService.findByLostId(txnId);
            }
            optional.ifPresentOrElse(result -> {
                consumer.accept(wb, result);
            }, () -> {
                logger.info("No Txn ID {} found for furthure processing for subFeature {}", txnId, subFeature);
                // searchImeiByPoliceMgmtRepository.updateStatus("DONE", txnId, null);
                webActionDbRepository.updateWebActionStatus(5, wb.getId());
                logger.info("Updated status as DONE in web_action_db for txn id {} in subFeature {}", txnId, subFeature);
            });
        }
    }

    @Override
    public void executeInit(WebActionDb wb) {
        String subFeatureName = wb.getSubFeature().trim();
        logger.info("Initialization started for MOI sub feaure : {}", subFeatureName);
        this.action(wb, subFeatureName);
    }

    @Override
    public void executeProcess(WebActionDb wb) {
        String subFeature = new StringBuilder(wb.getSubFeature()).toString();
        this.action(wb, subFeature);
    }

    @Override
    public void validateProcess(WebActionDb wb) {
        String subFeature = new StringBuilder(wb.getSubFeature()).toString();
        this.action(wb, subFeature);
    }
}

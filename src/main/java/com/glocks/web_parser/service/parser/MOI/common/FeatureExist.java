package com.glocks.web_parser.service.parser.MOI.common;

import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.service.parser.FeatureInterface;
import com.glocks.web_parser.service.parser.MOI.IMEI_SEARCH_RECOVERY.IMEISearchRecoverySubFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@Component
public class FeatureExist {
    private static IMEISearchRecoverySubFeature imeiSearchRecoverySubFeature;

    public FeatureExist(IMEISearchRecoverySubFeature imeiSearchRecoverySubFeature) {
        this.imeiSearchRecoverySubFeature = imeiSearchRecoverySubFeature;
    }

    static BiConsumer<WebActionDb, Object> task(String columnName) {
        Map<String, BiConsumer<WebActionDb, Object>> map = new HashMap<>();
        if (Objects.nonNull(columnName) && !columnName.isEmpty()) {
            map.put("IMEI_SEARCH_RECOVERY_INIT", (x, y) -> imeiSearchRecoverySubFeature.delegateInitRequest(x, (SearchImeiByPoliceMgmt) y));
            // map.put("IMEI_SEARCH_RECOVERY_EXECUTE", (x, y) -> new IMEISearchRecoverySubFeature().delegateExecuteProcess(x, (SearchImeiByPoliceMgmt) y));
            //map.put("IMEI_SEARCH_RECOVERY_VALIDATE", (x, y) -> new IMEISearchRecoverySubFeature().delegateValidateRequest(x, (SearchImeiByPoliceMgmt) y));

        }
        return map.get(columnName);
    }
}

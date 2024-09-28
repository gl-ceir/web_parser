package com.glocks.web_parser.service.parser.moi.utility;

import lombok.Getter;

@Getter
public enum ConfigurableParameter {
    ALERT_IMEI_SEARCH_RECOVERY("alert00001"),
    ALERT_LOST_STOLEN("alert00001"),
    ALERT_PENDING_VERIFICATION("alert00001"),
    PENDING_VERIFICATION_FEATURE("MOI"),
    PENDING_VERIFICATION_TAG("MOI_PENDING_VERIFICATION_MSG"),
    ALERT_RECOVER("alert00001");
    private String value;

    private ConfigurableParameter(String value) {
        this.value = value;
    }
}

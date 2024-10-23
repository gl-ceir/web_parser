package com.glocks.web_parser.service.parser.moi.utility;

import lombok.Getter;

@Getter
public enum ConfigurableParameter {
    SINGLE("SINGLE"),

    BULK("BULK"),

    ALERT_IMEI_SEARCH_RECOVERY("Alertt8005"),

    ALERT_LOST_STOLEN("Alertt8005"),

    ALERT_PENDING_VERIFICATION("Alertt8005"),

    ALERT_RECOVER("Alertt8005"),

    PENDING_VERIFICATION_STAGE_INIT("VERIFICATION_STAGE_INIT"),

    PENDING_VERIFICATION_STAGE_DONE("VERIFICATION_STAGE_DONE"),

    STOLEN_NOTIFICATION("STOLEN"),

    MOI_PENDING_VERIFICATION_MSG("MOI_PENDING_VERIFICATION_MSG");
    // MOI_PENDING_VERIFICATION_MSG("MOI_PENDING_VERIFICATION_MSG");

    private String value;

    private ConfigurableParameter(String value) {
        this.value = value;
    }
}

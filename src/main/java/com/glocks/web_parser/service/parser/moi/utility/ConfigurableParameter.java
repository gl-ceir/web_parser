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

    PENDING_VERIFICATION_STAGE_INIT("0"),

    PENDING_VERIFICATION_STAGE_DONE("1"),

    STOLEN_NOTIFICATION("STOLEN"),

    STOLEN_TAG_NOTIFICATION("stolen_success");

    private String value;

    private ConfigurableParameter(String value) {
        this.value = value;
    }
}

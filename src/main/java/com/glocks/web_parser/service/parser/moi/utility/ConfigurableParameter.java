package com.glocks.web_parser.service.parser.moi.utility;

import lombok.Getter;

@Getter
public enum ConfigurableParameter {
    SINGLE("SINGLE"),

    BULK("BULK"),

    FILE_MISSING_ALERT("Alertt8005"),

    PENDING_VERIFICATION_STAGE_INIT("VERIFICATION_STAGE_INIT"),

    PENDING_VERIFICATION_STAGE_DONE("VERIFICATION_STAGE_DONE"),

    STOLEN_NOTIFICATION("STOLEN"),

    MOI_PENDING_VERIFICATION_MSG("MOI_PENDING_VERIFICATION_MSG"),
    MOI_VERIFICATION_DONE_MSG("MOI_VERIFICATION_DONE_MSG");

    private String value;

    private ConfigurableParameter(String value) {
        this.value = value;
    }
}

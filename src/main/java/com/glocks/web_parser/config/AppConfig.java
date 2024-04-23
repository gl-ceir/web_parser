package com.glocks.web_parser.config;


import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class AppConfig {

    @Value("${ta.base.file.path}")
    String taBaseFilePath;


    @Value("${alert.url}")
    String alertUrl;

    @Value("${trc.ta.file.separator}")
    String trcTaFileSeparator;

    @Value("${qa.base.file.path}")
    String qaBaseFilePath;

    @Value("${trc.qa.file.separator}")
    String trcQaFileSeparator;

    @Value("${local.manufacturer.base.file.path}")
    String localManufacturerBaseFilePath;

    @Value("${trc.local.manufacturer.file.separator}")
    String trcLocalManufacturerFileSeparator;

    @Value("${list.mgmt.file.path}")
    String listMgmtFilePath;

    @Value("${list.mgmt.file.separator}")
    String listMgmtFileSeparator;

}

package com.glocks.web_parser.config;


import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class AppConfig {

    @Value("${ta.base.file.path}")
    String taBaseFilePath;
//    @Value("${ta.processed.base.file.path}")
//    String taProcessedBaseFilePath;

    @Value("${alert.url}")
    String alertUrl;

    @Value("${trc.ta.file.separator}")
    String trcTaFileSeparator;

    @Value("${qa.base.file.path}")
    String qaBaseFilePath;
//    @Value("${qa.processed.base.file.path}")
//    String qaProcessedBaseFilePath;
    @Value("${trc.qa.file.separator}")
    String trcQaFileSeparator;

    @Value("${local.manufacturer.base.file.path}")
    String localManufacturerBaseFilePath;

//    @Value("${local.manufacturer.processed.base.file.path}")
//    String localManufacturerProcessedBaseFilePath;

    @Value("${trc.local.manufacturer.file.separator}")
    String trcLocalManufacturerFileSeparator;

}

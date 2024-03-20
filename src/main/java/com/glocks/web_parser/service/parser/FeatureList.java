package com.glocks.web_parser.service.parser;

import com.glocks.web_parser.service.parser.Recovery.RecoveryFeature;
import com.glocks.web_parser.service.parser.Stolen.StolenFeature;
import com.glocks.web_parser.service.parser.TRC.TRCFeature;

import java.util.Map;

public interface FeatureList {

    static Map<String, FeatureInterface> getFeatures() {
        return Map.of("TRC", new TRCFeature(),
                "Stolen", new StolenFeature(),
                "Recovery", new RecoveryFeature()
        );
    }
}


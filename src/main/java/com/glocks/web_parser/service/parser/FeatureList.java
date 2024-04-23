package com.glocks.web_parser.service.parser;

import com.glocks.web_parser.service.parser.Recovery.RecoveryFeature;
import com.glocks.web_parser.service.parser.Stolen.StolenFeature;
import com.glocks.web_parser.service.parser.TRC.TRCFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FeatureList {

    @Autowired
    TRCFeature trcFeature;

    public Map<String, FeatureInterface> getFeatures() {
        return Map.of("TRCManagement", trcFeature,
                "Stolen", new StolenFeature(),
                "Recovery", new RecoveryFeature()
        );
    }
}


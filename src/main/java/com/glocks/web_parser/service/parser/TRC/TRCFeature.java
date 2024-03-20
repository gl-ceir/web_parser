package com.glocks.web_parser.service.parser.TRC;

import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.service.parser.FeatureInterface;

public class TRCFeature implements FeatureInterface {
    @Override
    public void executeInit(WebActionDb wb) {
        System.out.println("This is trc init flow");

    }
    @Override
    public void executeProcess(WebActionDb wb) {
        System.out.println("This is trc init flow");

    }
}

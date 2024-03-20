package com.glocks.web_parser.service.parser.TRC;

import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.service.parser.FeatureInterface;

public class TRCFeature implements FeatureInterface {
    @Override
    public void executeInit(WebActionDb wb) {
        System.out.println("This is trc init flow");

        // file REe

        if(wb.getSubFeature() == "TAData")
          // ApproveTrcSubFeature
           // else


    }
    @Override
    public void executeProcess(WebActionDb wb) {
         // Mark Done
        System.out.println("This is trc init flow");
    }
}

package com.glocks.web_parser.controller;

import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.service.parser.FeatureInterface;
import com.glocks.web_parser.service.parser.FeatureList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;


//@Autowired
//PropertiesReader propertiesReader;

//@Autowired
//FeatureFactory featureFactory;

@Component
public class MainController {
    private final Logger logger = LogManager.getLogger(MainController.class);

    public void startProcess(ApplicationContext context) {
        var wb = new WebActionDb();  //getWebAction Details // 1:
        wb.setFeature("TRCMgmt");
        wb.setState(0);


        String state = wb.getState() == 0 ? "init" : "";

        System.out.println("********** Process Started ********");
        FeatureList.getFeatures()
                .entrySet()
                .stream()
                .filter(a -> a.toString().contains(wb.getFeature()))
                .map(Map.Entry::getValue)
                .map(FeatureInterface.class::cast)
                .reduce(new String(), (result, ruleNode) -> {
                    if (state.contains("init")) {
                        ruleNode.executeInit(wb);
                    } else {
                        ruleNode.executeProcess(wb);
                    }
                    return result;
                }, (cumulative, intermediate) -> {
                    return intermediate;
                });


    }
}


// return RulesList.getItems()
//                .stream()
//                .filter(a -> a.getClass().getName().toString().contains(ruleEngine.ruleName))
//        .map(ExecutionInterface.class::cast)
//                .reduce(new String(), (result, ruleNode) -> {
//String key = ruleEngine.executeRuleAction;
//                    if (key.contains("executeRule")) {
//result = ruleNode.executeRule(ruleEngine);
//                    } else {
//result = ruleNode.executeAction(ruleEngine);
//                    }
//                            return result;
//                }, (cumulative, intermediate) -> {
//        return intermediate;
//                });
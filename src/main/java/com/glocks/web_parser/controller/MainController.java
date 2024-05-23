package com.glocks.web_parser.controller;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.WebActionDbRepository;
import com.glocks.web_parser.service.parser.FeatureInterface;
import com.glocks.web_parser.service.parser.FeatureList;
import com.glocks.web_parser.service.parser.ListMgmt.ListMgmtFeature;
import com.glocks.web_parser.service.parser.TRC.TRCFeature;
import com.glocks.web_parser.utils.VirtualIpAddressUtil;
import lombok.Synchronized;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Synchronize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Component
public class MainController {

    @Autowired
    AppConfig appConfig;

    @Autowired
    WebActionDbRepository webActionDbRepository;

    @Autowired
    TRCFeature trcFeature;
    @Autowired
    ListMgmtFeature listMgmtFeature;

    @Autowired
    FeatureList featureList;
    @Autowired
    VirtualIpAddressUtil virtualIpAddressUtil;

    private final Logger logger = LogManager.getLogger(MainController.class);
    AtomicInteger isRunning = new AtomicInteger(0);


    @Scheduled(cron = "${web.parser.sleep.time}")
    public void listPendingProcessTask() throws InterruptedException {
        // check if vip here or not, if yes then process
        while (true) {
            if ( virtualIpAddressUtil.getFullList()) {  //remove !
                break;
                //     break; // Server is active, no need to keep checking
            } else {
                logger.info("VIP not found. Sleeping for " + appConfig.getWebParserSleepTime() + " seconds.");
                return;
            }
        }


        if(isRunning.get() ==  1) {
            logger.info("Process already running...");
        }
        else {
            logger.info("Starting the web parser process.");
            List<WebActionDb> listOfPendingTasks = webActionDbRepository.getListOfPendingTasks(
                    appConfig.getFeatureList(), appConfig.getWebParserQueryGap());
            logger.info(listOfPendingTasks);
            if (listOfPendingTasks.isEmpty()) {
                logger.info("No tasks to perform");
            } else {
                for (WebActionDb webActionDb : listOfPendingTasks) {
                    startProcess(webActionDb);
                }
            }
            isRunning.set(0);
        }

    }
    public void startProcess(WebActionDb wb) {

        logger.info("Starting process for the entry in web_action_db {}", wb);
        String state = wb.getState() == 1 ? "init" : wb.getState() == 2 ? "validateProcess" :
                wb.getState() == 3 ? "executeProcess" : "";
        if(state.isEmpty()) {
            logger.error("The web_action_db entry does not have the state column value populated.");
            return;
        }
        featureList.getFeatures()
                .entrySet()
                .stream()
                .filter(a -> a.toString().contains(wb.getFeature()))
                .map(Map.Entry::getValue)
                .map(FeatureInterface.class::cast)
                .reduce(new String(), (result, ruleNode) -> {
                    if (state.contains("init")) {
                        ruleNode.executeInit(wb);
                    } else if(state.contains("validateProcess")){
                        ruleNode.validateProcess(wb);
                    } else {
                        ruleNode.executeProcess(wb);
                    }
                    return result;
                }, (cumulative, intermediate) -> {
                    return intermediate;
                });


    }

    private static void sleepForSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
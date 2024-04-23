package com.glocks.web_parser.service.operatorSeries;


import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.OperatorSeries;
import com.glocks.web_parser.repository.app.OperatorSeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class OperatorSeriesService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    AppConfig appConfig;
    @Autowired
    OperatorSeriesRepository operatorSeriesRepository;

    HashMap<String, String> operatorSeriesHash;
    public void fillOperatorSeriesHash() {

        // populate the hash map of operator list
        List<OperatorSeries> allOperatorSeries = operatorSeriesRepository.findAll();
        logger.info("All the entries of operator Series {}", allOperatorSeries);
        operatorSeriesHash = new HashMap<String, String>();
        for(OperatorSeries operatorSeries: allOperatorSeries) {
            int j = operatorSeries.getSeriesStart();
            while(j <= operatorSeries.getSeriesEnd()) {
                operatorSeriesHash.put(String.valueOf(j), operatorSeries.getOperatorName());
                j++;
            }
        }
    }

    public String getOperatorName(String imsi, String msisdn) {

        boolean found = false;
        for (String key : operatorSeriesHash.keySet()) {
            if (imsi.startsWith(key)) {
                logger.info("imsi prefix matched with operator {}", operatorSeriesHash.get(key));
                return operatorSeriesHash.get(key);
            }
            if(msisdn.startsWith(key)) {
                logger.info("msisdn prefix matched with operator {}", operatorSeriesHash.get(key));
                return operatorSeriesHash.get(key);
            }
        }
        return "";
    }


}

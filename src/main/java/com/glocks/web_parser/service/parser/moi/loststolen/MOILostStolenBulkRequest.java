package com.glocks.web_parser.service.parser.moi.loststolen;

import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import com.glocks.web_parser.service.parser.moi.utility.RequestTypeHandler;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class MOILostStolenBulkRequest implements RequestTypeHandler<LostDeviceMgmt> {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final WebActionDbRepository webActionDbRepository;
    private final MOIService moiService;
    private final BlackListRepository blackListRepository;
    private final ImeiPairDetailRepository imeiPairDetailRepository;
    private final ImeiPairDetailHisRepository imeiPairDetailHisRepository;
    private final BlackListHisRepository blackListHisRepository;
    private final GreyListHisRepository greyListHisRepository;
    private final GreyListRepository greyListRepository;

    Map<String, Integer> map = new HashMap<>();

    @Override
    public void executeInitProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        executeValidateProcess(webActionDb, lostDeviceMgmt);
    }


    @Override
    public void executeValidateProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {
        try {
            int greyListDuration = Integer.parseInt(moiService.greyListDuration());
            map.put("greyListDuration", greyListDuration);
            executeProcess(webActionDb, lostDeviceMgmt);
        } catch (NumberFormatException e) {
            logger.info("Invalid value found for GREY_LIST_DURATION tag");
        }
    }

    @Override
    public void executeProcess(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt) {


    }
}
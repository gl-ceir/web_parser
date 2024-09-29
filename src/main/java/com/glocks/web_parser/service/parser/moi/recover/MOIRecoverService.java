package com.glocks.web_parser.service.parser.moi.recover;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.BlackListHis;
import com.glocks.web_parser.model.app.GreyListHis;
import com.glocks.web_parser.model.app.LostDeviceDetailHis;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.service.parser.moi.utility.IMEISeriesModel;
import com.glocks.web_parser.service.parser.moi.utility.MOIService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MOIRecoverService {
    private Logger logger = LogManager.getLogger(this.getClass());
    private final LostDeviceDetailRepository lostDeviceDetailRepository;
    private final LostDeviceDetailHisRepository lostDeviceDetailHisRepository;
    private final AppConfig appConfig;
    private final BlackListRepository blackListRepository;
    private final BlackListHisRepository blackListHisRepository;
    private final GreyListRepository greyListRepository;
    private final GreyListHisRepository greyListHisRepository;
    private final MOIService moiService;
    static String runningImei = null;

    public boolean fileValidation(String filePath, LostDeviceMgmt lostDeviceMgmt) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String record;
            IMEISeriesModel imeiSeriesModel = new IMEISeriesModel();
            String[] split;
            boolean headerSkipped = false;
            while ((record = reader.readLine()) != null) {
                if (!record.trim().isEmpty()) {
                    if (!headerSkipped) {
                        headerSkipped = true;
                    } else {
                        split = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        imeiSeriesModel.setImeiSeries(split);
                        logger.info("LostDeviceMgmt : {}", imeiSeriesModel);
                        List<String> imeiList = moiService.imeiSeries.apply(imeiSeriesModel);
                        if (imeiList.size() > 0) imeiList.forEach(imei -> {
                            runningImei = imei;
                            boolean isImeiValid = moiService.isNumericAndValid.test(imei);
                            if (!isImeiValid) {
                                logger.info("Invalid IMEI {}", imei);
                            } else {
                                blackListFlow(imei);
                                greyListFlow(imei);
                                lostDeviceDetailFlow(imei, lostDeviceMgmt);
                            }
                        });
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Exception in processing the file {}", ex.getMessage());
            return false;
        }
        return true;
    }


    public void blackListFlow(String imei) {
        moiService.findBlackListByImei(imei).ifPresent(response -> {
            String source = response.getSource();
            int val = (int) moiService.sourceCount.apply(source).longValue();
            switch (val) {
                case 1 -> {
                    BlackListHis blackListHis = new BlackListHis();
                    BeanUtils.copyProperties(response, blackListHis);
                    logger.info("BlackListHis : {}", blackListHis);
                    BlackListHis save = moiService.save(blackListHis, blackListHisRepository::save);
                    logger.info("black_list id {}", response.getId());
                    if (save != null) blackListRepository.deleteById(Math.toIntExact(response.getId()));
                }
                case 2 -> {
                    String updatedSourceValue = moiService.remove(source);
                    moiService.updateSource(updatedSourceValue, imei, "BLACK_LIST");
                }
                default -> logger.info("No valid source value {}  found", source);
            }
        });
    }

    public void greyListFlow(String imei) {
        moiService.findGreyListByImei(imei).ifPresent(response -> {
            String source = response.getSource();
            int val = (int) moiService.sourceCount.apply(source).longValue();
            switch (val) {
                case 1 -> {
                    if (source.equals("MOI")) {
                        GreyListHis greyListHis = new GreyListHis();
                        BeanUtils.copyProperties(response, greyListHis);
                        logger.info("GreyListHis {}", greyListHis);
                        GreyListHis save = moiService.save(greyListHis, greyListHisRepository::save);
                        logger.info("grey_list id {}", response.getId());
                        if (save != null) greyListRepository.deleteById(response.getId());
                    }
                }
                case 2 -> {
                    String updatedSourceValue = moiService.remove(source);
                    moiService.updateSource(updatedSourceValue, imei, "GREY_LIST");
                }
                default -> logger.info("No valid source value {}  found in grey_list", source);
            }
        });
    }

    public void lostDeviceDetailFlow(String imei, LostDeviceMgmt lostDeviceMgmt) {
        LostDeviceDetailHis lostDeviceDetailHis = LostDeviceDetailHis.builder().imei(imei).contactNumber(lostDeviceMgmt.getContactNumber()).requestId(lostDeviceMgmt.getLostId()).status("ADD").requestType("RECOVER").build();
        LostDeviceDetailHis save = moiService.save(lostDeviceDetailHis, lostDeviceDetailHisRepository::save);
        if (save != null) lostDeviceDetailRepository.deleteByImeiAndStatusIn(imei, List.of("STOLEN", "LOST"));
    }


}

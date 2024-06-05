package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.GreyList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GreyListRepository extends JpaRepository<GreyList, Long> {



    GreyList findGreyListByImeiAndImsi(String imei, String imsi);
    GreyList findGreyListByImsi(String imsi);

    GreyList findGreyListByImei(String imei);
}

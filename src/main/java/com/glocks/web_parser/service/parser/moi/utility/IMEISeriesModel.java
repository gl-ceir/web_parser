package com.glocks.web_parser.service.parser.moi.utility;

import com.glocks.web_parser.validator.Validation;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Data
public class IMEISeriesModel {
    private String imei1;
    private String imei2;
    private String imei3;
    private String imei4;
    Map<String, String> map = new LinkedHashMap<>();
    @Autowired
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Validation validation;

    public IMEISeriesModel() {

    }

    public IMEISeriesModel setImeiSeries(String[] record) {
        for (int i = 0; i < record.length; i++) {
            switch (i) {
                case 0 -> {
                    this.imei1 = recordIndexWise(record[i]);
                    map.put("imei1", this.imei1);
                }
                case 1 -> {
                    this.imei2 = recordIndexWise(record[i]);
                    map.put("imei2", this.imei2);
                }
                case 2 -> {
                    this.imei3 = recordIndexWise(record[i]);
                    map.put("imei3", this.imei3);
                }
                case 3 -> {
                    this.imei4 = recordIndexWise(record[i]);
                    map.put("imei4", this.imei4);
                }
            }
        }
        return this;
    }

    public String recordIndexWise(String input) {
        return Objects.nonNull(input) ? input.strip() : null;
    }

    public Map<String, String> toMap() {
        return map;
    }
}

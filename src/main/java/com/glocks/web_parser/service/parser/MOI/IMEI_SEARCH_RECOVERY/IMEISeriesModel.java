package com.glocks.web_parser.service.parser.MOI.IMEI_SEARCH_RECOVERY;

import lombok.Data;

import java.util.Objects;

@Data
public class IMEISeriesModel {
    private String imei1;
    private String imei2;
    private String imei3;
    private String imei4;

    public IMEISeriesModel(String[] record) {
        for (int i = 0; i < record.length; i++) {
            switch (i) {
                case 0 -> this.imei1 = recordIndexWise(record[i]);
                case 1 -> this.imei2 = recordIndexWise(record[i]);
                case 2 -> this.imei3 = recordIndexWise(record[i]);
                case 3 -> this.imei4 = recordIndexWise(record[i]);
            }
        }
    }

    public String recordIndexWise(String input) {
        return Objects.nonNull(input) ? input.strip() : null;
    }

    public boolean areAllFieldsEmpty() {
        return imei1.isEmpty() && imei2.isEmpty() && imei3.isEmpty() && imei4.isEmpty();
    }

}

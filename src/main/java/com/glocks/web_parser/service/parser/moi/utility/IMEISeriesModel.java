package com.glocks.web_parser.service.parser.moi.utility;

import com.glocks.web_parser.validator.Validation;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

@Data
public class IMEISeriesModel {
    private String imei1;
    private String imei2;
    private String imei3;
    private String imei4;
    @Autowired
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Validation validation;

    public IMEISeriesModel setImeiSeries(String[] record, String feature) {
        switch (feature) {
            case "STOLEN" -> {
                for (int i = 1; i <= 4; i++) {
                    switch (i) {
                        case 1 -> {
                            this.imei1 = recordIndexWise(record[i]);
                        }
                        case 2 -> {
                            this.imei2 = recordIndexWise(record[i]);
                        }
                        case 3 -> {
                            this.imei3 = recordIndexWise(record[i]);
                        }
                        case 4 -> {
                            this.imei4 = recordIndexWise(record[i]);
                        }
                    }
                }
            }
            case "DEFAULT" -> {

                for (int i = 0; i < record.length; i++) {
                    switch (i) {
                        case 0 -> {
                            this.imei1 = recordIndexWise(record[i]);
                        }
                        case 1 -> {
                            this.imei2 = recordIndexWise(record[i]);
                        }
                        case 2 -> {
                            this.imei3 = recordIndexWise(record[i]);
                        }
                        case 3 -> {
                            this.imei4 = recordIndexWise(record[i]);
                        }
                    }
                }
            }
        }

        return this;
    }

    public String recordIndexWise(String input) {
        return Objects.nonNull(input) ? input.strip() : null;
    }
}

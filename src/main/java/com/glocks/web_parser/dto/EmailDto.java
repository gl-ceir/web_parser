package com.glocks.web_parser.dto;

import lombok.Data;

@Data
public class EmailDto {

    String email;
    String message;
    String subject;
    String language;
    String txn_id;
    String file;

}

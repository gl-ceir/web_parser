package com.glocks.web_parser.model.app;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "grey_list_his")
public class GreyListHis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on")
    LocalDateTime createdOn;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified_on")
    LocalDateTime modifiedOn;

    @Column(name = "imsi")
    String imsi;

    @Column(name = "msisdn")
    String msisdn;

    @Column(name = "imei")
    String imei;

    @Column(name = "mode_type")
    String modeType;


    @Column(name = "request_type")
    String requestType;

    @Column(name = "txn_id")
    String txnId;

    @Column(name = "user_id")
    String userId;

    @Column(name = "operator_name")
    String operatorName;

    @Column(name = "actual_imei")
    String actualImei;

    @Column(name = "tac")
    String tac;
    @Column(name = "expiry_date")
    LocalDateTime expiryDate;
    @Column(name = "remark")
    String remarks;

    @Column(name = "operation")
    int operation;
    @Column(name = "source")
    String source;

    @Column(name = "action")
    String action;
}

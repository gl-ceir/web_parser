package com.glocks.web_parser.model.app;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_imei_detail_by_police")
@Data
public class SearchImeiDetailByPolice implements Serializable {
    private static final long serialVersionUID = -2772314216410962795L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "modified_on")
    private LocalDateTime modifiedOn;

    @Column(name = "imei", nullable = false)
    private String imei;

    @Column(name = "lost_date_time")
    private LocalDateTime lostDateTime;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "device_owner_name")
    private String deviceOwnerName;

    @Column(name = "device_owner_address")
    private String deviceOwnerAddress;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "device_owner_national_id")
    private String deviceOwnerNationalId;

    @Column(name = "device_lost_police_station")
    private String deviceLostPoliceStation;

    @Column(name = "request_mode")
    private String requestMode;

}

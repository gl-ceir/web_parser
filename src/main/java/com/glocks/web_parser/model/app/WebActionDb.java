package com.glocks.web_parser.model.app;


//import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;


@Getter
@Setter
// @Entity
@ToString
@AllArgsConstructor
@NoArgsConstructor
// @Table(name = "web_transaction_detail")
public class WebActionDb implements Serializable {

    private static final long serialVersionUID = 1L;

   // @Id
  //  @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String feature ;

    private String subFeature;

    private String txnId;

    private int state;

    private String data;


}

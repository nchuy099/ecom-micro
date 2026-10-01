package com.nchuy099.ecom_micro_product_service.consumers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
//@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {


    private String id;

    private String customerId;

    private String status;

    private Integer totalAmount;

}

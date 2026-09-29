package com.nchuy099.ecom_micro_order_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductResp {

    private String id;

    private String name;
    private Integer price;
    private Integer stock;
}

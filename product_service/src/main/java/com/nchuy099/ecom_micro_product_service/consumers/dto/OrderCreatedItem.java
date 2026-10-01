package com.nchuy099.ecom_micro_product_service.consumers.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderCreatedItem {
    private String productId;
    private Integer quantity;
}

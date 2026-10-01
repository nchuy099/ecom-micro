package com.nchuy099.ecom_micro_product_service.consumers.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderCreatedEvent {
    private String id;
    private String customerId;
    private String status;
    private Integer totalAmount;
    private List<OrderCreatedItem> orderItems;
}

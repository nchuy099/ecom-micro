package com.nchuy099.ecom_micro_order_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private String id;
    private String customerId;
    private String status;
    private Integer totalAmount;
    private List<OrderCreatedItem> orderItems;
}

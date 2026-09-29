package com.nchuy099.ecom_micro_order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderReq {
    @NotEmpty
    private String customerId;
    @NotEmpty
    private List<@Valid CreateOrderItemReq> items;
}

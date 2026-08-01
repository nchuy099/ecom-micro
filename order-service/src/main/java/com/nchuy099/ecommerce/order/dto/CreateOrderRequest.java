package com.nchuy099.ecommerce.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotEmpty(message = "Order items must not be empty")
        @Valid
        List<OrderItemRequest> items
) {
}

package com.nchuy099.ecommerce.order.client.dto;

public record OrderStockReservationRequest(
        Long orderId,
        Long userId,
        Integer quantity
) {
}

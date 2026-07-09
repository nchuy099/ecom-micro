package com.nchuy099.ecommerce.common.event;

public record OrderItemRequested(
        Long productId,
        Integer quantity
) {
}

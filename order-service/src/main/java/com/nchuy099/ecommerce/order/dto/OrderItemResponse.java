package com.nchuy099.ecommerce.order.dto;

import java.math.BigDecimal;

import com.nchuy099.ecommerce.order.entity.OrderItemEntity;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItemEntity entity) {
        return new OrderItemResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getSubtotal()
        );
    }
}

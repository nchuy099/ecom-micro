package com.nchuy099.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.nchuy099.ecommerce.order.entity.OrderEntity;
import com.nchuy099.ecommerce.order.entity.OrderStatus;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getOrderNumber(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getItems() != null
                        ? entity.getItems().stream().map(OrderItemResponse::from).toList()
                        : List.of(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

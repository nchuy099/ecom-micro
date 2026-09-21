package com.nchuy099.ecommerce.order.event;

import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        String eventType,
        Long orderId,
        Long userId,
        String orderNumber,
        String message,
        Instant occurredAt
) {
    public static OrderConfirmedEvent of(Long orderId, Long userId, String orderNumber, String message) {
        return new OrderConfirmedEvent(
                UUID.randomUUID(),
                "ORDER_CONFIRMED",
                orderId,
                userId,
                orderNumber,
                message,
                Instant.now()
        );
    }
}

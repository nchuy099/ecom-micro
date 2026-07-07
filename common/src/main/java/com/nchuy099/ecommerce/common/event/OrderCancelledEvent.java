package com.nchuy099.ecommerce.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        Long orderId,
        Long userId,
        Instant cancelledAt
) {
    public static OrderCancelledEvent of(Long orderId, Long userId) {
        return new OrderCancelledEvent(
                UUID.randomUUID(),
                KafkaTopics.ORDER_CANCELLED,
                String.valueOf(orderId),
                orderId,
                userId,
                Instant.now()
        );
    }
}

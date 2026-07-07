package com.nchuy099.ecommerce.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        Long orderId,
        Long userId,
        List<OrderItemRequested> items,
        Instant createdAt
) {
    public static OrderCreatedEvent of(Long orderId, Long userId) {
        return of(orderId, userId, List.of());
    }

    public static OrderCreatedEvent of(Long orderId, Long userId, List<OrderItemRequested> items) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                KafkaTopics.ORDER_CREATED,
                String.valueOf(orderId),
                orderId,
                userId,
                List.copyOf(items),
                Instant.now()
        );
    }
}

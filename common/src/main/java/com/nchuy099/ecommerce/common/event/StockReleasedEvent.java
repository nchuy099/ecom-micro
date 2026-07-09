package com.nchuy099.ecommerce.common.event;

import java.time.Instant;
import java.util.UUID;

public record StockReleasedEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        Long orderId,
        Long userId,
        Instant releasedAt
) {
    public static StockReleasedEvent of(Long orderId, Long userId) {
        return new StockReleasedEvent(
                UUID.randomUUID(),
                KafkaTopics.STOCK_RELEASED,
                String.valueOf(orderId),
                orderId,
                userId,
                Instant.now()
        );
    }
}

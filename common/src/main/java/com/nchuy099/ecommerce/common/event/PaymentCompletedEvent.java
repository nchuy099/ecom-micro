package com.nchuy099.ecommerce.common.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        Long orderId,
        Long userId,
        Instant completedAt
) {
    public static PaymentCompletedEvent of(Long orderId, Long userId) {
        return new PaymentCompletedEvent(
                UUID.randomUUID(),
                KafkaTopics.PAYMENT_COMPLETED,
                String.valueOf(orderId),
                orderId,
                userId,
                Instant.now()
        );
    }
}

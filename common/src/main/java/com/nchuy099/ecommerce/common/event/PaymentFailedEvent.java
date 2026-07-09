package com.nchuy099.ecommerce.common.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        Long orderId,
        Long userId,
        String reason,
        Instant failedAt
) {
    public static PaymentFailedEvent of(Long orderId, Long userId, String reason) {
        return new PaymentFailedEvent(
                UUID.randomUUID(),
                KafkaTopics.PAYMENT_FAILED,
                String.valueOf(orderId),
                orderId,
                userId,
                reason,
                Instant.now()
        );
    }
}

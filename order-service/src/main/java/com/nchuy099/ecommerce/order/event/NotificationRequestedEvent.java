package com.nchuy099.ecommerce.order.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationRequestedEvent(
        UUID eventId,
        String eventType,
        String campaignId,
        Long userId,
        String subject,
        String message,
        Instant requestedAt
) {
    public static NotificationRequestedEvent orderConfirmed(
            Long orderId,
            Long userId,
            String orderNumber,
            String message
    ) {
        return new NotificationRequestedEvent(
                UUID.randomUUID(),
                KafkaTopics.NOTIFICATION_REQUESTED,
                "order:" + orderId,
                userId,
                "Order confirmed: " + orderNumber,
                message,
                Instant.now()
        );
    }
}

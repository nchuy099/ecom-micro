package com.nchuy099.ecommerce.common.event;

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
    public static NotificationRequestedEvent of(String campaignId, Long userId, String subject, String message) {
        return new NotificationRequestedEvent(
                UUID.randomUUID(),
                KafkaTopics.NOTIFICATION_REQUESTED,
                campaignId,
                userId,
                subject,
                message,
                Instant.now()
        );
    }

    public static NotificationRequestedEvent forOrderCreated(OrderCreatedEvent event) {
        return of(
                "order-" + event.orderId(),
                event.userId(),
                "Order " + event.orderId() + " created",
                "Order " + event.orderId() + " is pending confirmation"
        );
    }
}

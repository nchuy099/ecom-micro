package com.nchuy099.ecommerce.notification.event;

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

}

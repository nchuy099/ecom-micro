package com.nchuy099.ecommerce.common.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationChannelRequestedEvent(
        UUID eventId,
        String eventType,
        String campaignId,
        Long userId,
        NotificationChannel channel,
        String subject,
        String message,
        Instant requestedAt
) {
    public static NotificationChannelRequestedEvent of(NotificationRequestedEvent request, NotificationChannel channel) {
        return new NotificationChannelRequestedEvent(
                UUID.randomUUID(),
                topicFor(channel),
                request.campaignId(),
                request.userId(),
                channel,
                request.subject(),
                request.message(),
                Instant.now()
        );
    }

    public static String topicFor(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> KafkaTopics.NOTIFICATION_EMAIL_REQUESTED;
            case PUSH -> KafkaTopics.NOTIFICATION_PUSH_REQUESTED;
            case SMS -> KafkaTopics.NOTIFICATION_SMS_REQUESTED;
        };
    }
}

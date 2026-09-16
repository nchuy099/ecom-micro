package com.nchuy099.ecommerce.notification.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationDlqEvent(
        UUID eventId,
        String eventType,
        String campaignId,
        Long userId,
        NotificationChannel channel,
        String failureReason,
        Integer attempts,
        Instant failedAt
) {
    public static NotificationDlqEvent of(
            String campaignId,
            Long userId,
            NotificationChannel channel,
            String failureReason,
            Integer attempts
    ) {
        return new NotificationDlqEvent(
                UUID.randomUUID(),
                topicFor(channel),
                campaignId,
                userId,
                channel,
                failureReason,
                attempts,
                Instant.now()
        );
    }

    public static String topicFor(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> KafkaTopics.NOTIFICATION_EMAIL_DLQ;
            case PUSH -> KafkaTopics.NOTIFICATION_PUSH_DLQ;
            case SMS -> KafkaTopics.NOTIFICATION_SMS_DLQ;
        };
    }
}

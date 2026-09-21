package com.nchuy099.ecommerce.order.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationTask(
        UUID taskId,
        String eventType,
        String sourceType,
        String idempotencyKey,
        String campaignId,
        Long userId,
        NotificationChannel channel,
        String subject,
        String message,
        int priority,
        int attempt,
        Instant scheduledAt
) {
    public static NotificationTask flashSale(
            Long campaignId,
            Long userId,
            NotificationChannel channel,
            String subject,
            String message,
            int priority,
            Instant scheduledAt
    ) {
        String campaignKey = "flash-sale:" + campaignId;
        return new NotificationTask(
                UUID.randomUUID(),
                "NOTIFICATION_TASK",
                "FLASH_SALE_CAMPAIGN",
                campaignKey + ":" + userId + ":" + channel,
                campaignKey,
                userId,
                channel,
                subject,
                message,
                priority,
                0,
                scheduledAt
        );
    }

    public NotificationTask retry(int nextAttempt) {
        return new NotificationTask(
                taskId, eventType, sourceType, idempotencyKey, campaignId, userId,
                channel, subject, message, priority, nextAttempt, scheduledAt
        );
    }
}

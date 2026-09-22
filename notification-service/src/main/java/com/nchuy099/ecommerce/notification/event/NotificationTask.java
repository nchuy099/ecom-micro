package com.nchuy099.ecommerce.notification.event;

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
    public static NotificationTask orderConfirmed(
            OrderConfirmedEvent event,
            NotificationChannel channel
    ) {
        String campaignId = "order:" + event.orderId();
        return new NotificationTask(
                UUID.randomUUID(),
                "NOTIFICATION_TASK",
                "ORDER_CONFIRMED",
                campaignId + ":" + event.userId() + ":" + channel,
                campaignId,
                event.userId(),
                channel,
                "Order confirmed: " + event.orderNumber(),
                event.message(),
                0,
                0,
                Instant.now()
        );
    }

    public NotificationTask retry(int nextAttempt) {
        return new NotificationTask(
                taskId, eventType, sourceType, idempotencyKey, campaignId, userId,
                channel, subject, message, priority, nextAttempt, scheduledAt
        );
    }
}

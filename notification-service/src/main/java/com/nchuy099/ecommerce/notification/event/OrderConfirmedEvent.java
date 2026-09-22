package com.nchuy099.ecommerce.notification.event;

import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        String eventType,
        Long orderId,
        Long userId,
        String orderNumber,
        String message,
        Instant occurredAt
) {
}

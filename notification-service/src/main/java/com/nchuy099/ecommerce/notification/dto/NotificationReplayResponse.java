package com.nchuy099.ecommerce.notification.dto;

public record NotificationReplayResponse(
        Long dlqId,
        String status,
        Boolean alreadySent
) {
}

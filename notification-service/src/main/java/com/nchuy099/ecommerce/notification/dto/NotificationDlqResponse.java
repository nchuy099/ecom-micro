package com.nchuy099.ecommerce.notification.dto;

import java.time.Instant;

import com.nchuy099.ecommerce.notification.event.NotificationChannel;
import com.nchuy099.ecommerce.notification.entity.NotificationDlqEntity;

public record NotificationDlqResponse(
        Long id,
        String campaignId,
        Long userId,
        NotificationChannel channel,
        String failureReason,
        Integer attempts,
        String status,
        Instant createdAt
) {
    public static NotificationDlqResponse from(NotificationDlqEntity entity) {
        return new NotificationDlqResponse(
                entity.getId(),
                entity.getCampaignId(),
                entity.getUserId(),
                entity.getChannel(),
                entity.getFailureReason(),
                entity.getAttempts(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }
}

package com.nchuy099.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;

public record FlashSaleCampaignResponse(
        Long campaignId,
        Long productId,
        Integer stock,
        Instant startsAt,
        Instant endsAt,
        BigDecimal promoPrice,
        Integer maxPerUser,
        int recipientCount,
        Instant notificationAt
) {
    public static FlashSaleCampaignResponse of(FlashSaleCampaignEntity campaign, int recipientCount, Instant notificationAt) {
        return new FlashSaleCampaignResponse(
                campaign.getId(), campaign.getProductId(), campaign.getStock(), campaign.getStartsAt(),
                campaign.getEndsAt(), campaign.getPromoPrice(), campaign.getMaxPerUser(), recipientCount, notificationAt
        );
    }
}

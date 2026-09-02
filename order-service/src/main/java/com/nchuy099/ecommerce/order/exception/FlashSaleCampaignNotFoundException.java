package com.nchuy099.ecommerce.order.exception;

public class FlashSaleCampaignNotFoundException extends RuntimeException {
    public FlashSaleCampaignNotFoundException(Long campaignId) {
        super("Flash sale campaign not found: " + campaignId);
    }
}

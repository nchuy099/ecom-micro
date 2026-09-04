package com.nchuy099.ecommerce.order.service;

import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;

public interface FlashSalePurchaseGate {
    FlashSalePurchaseResult tryPurchase(FlashSaleCampaignEntity campaign, Long userId, int quantity);
}

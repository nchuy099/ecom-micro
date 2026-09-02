package com.nchuy099.ecommerce.order.dto;

public record FlashSalePurchaseResponse(
        Long campaignId,
        Long productId,
        Long orderId,
        String status
) {
}

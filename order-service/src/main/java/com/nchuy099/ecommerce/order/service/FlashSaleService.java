package com.nchuy099.ecommerce.order.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.FlashSalePurchaseResponse;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import com.nchuy099.ecommerce.order.exception.FlashSaleCampaignNotFoundException;
import com.nchuy099.ecommerce.order.exception.FlashSalePurchaseRejectedException;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRepository;
import org.springframework.stereotype.Service;

@Service
public class FlashSaleService {
    private static final int PURCHASE_QUANTITY = 1;

    private final FlashSaleCampaignRepository campaignRepository;
    private final FlashSalePurchaseGate purchaseGate;
    private final OrderService orderService;
    private final Clock clock;

    public FlashSaleService(
            FlashSaleCampaignRepository campaignRepository,
            FlashSalePurchaseGate purchaseGate,
            OrderService orderService,
            Clock clock
    ) {
        this.campaignRepository = campaignRepository;
        this.purchaseGate = purchaseGate;
        this.orderService = orderService;
        this.clock = clock;
    }

    public FlashSalePurchaseResponse purchase(Long campaignId, Long userId) {
        FlashSaleCampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new FlashSaleCampaignNotFoundException(campaignId));

        Instant now = clock.instant();
        if (!campaign.isActiveAt(now)) {
            throw new FlashSalePurchaseRejectedException(
                    FlashSalePurchaseRejectedException.Reason.NOT_ACTIVE,
                    "Flash sale campaign is not active"
            );
        }

        FlashSalePurchaseResult result = purchaseGate.tryPurchase(campaign, userId, PURCHASE_QUANTITY);
        if (result == FlashSalePurchaseResult.SOLD_OUT) {
            throw new FlashSalePurchaseRejectedException(
                    FlashSalePurchaseRejectedException.Reason.SOLD_OUT,
                    "Flash sale campaign is sold out"
            );
        }
        if (result == FlashSalePurchaseResult.ALREADY_PURCHASED) {
            throw new FlashSalePurchaseRejectedException(
                    FlashSalePurchaseRejectedException.Reason.ALREADY_PURCHASED,
                    "Flash sale purchase limit already reached"
            );
        }

        OrderResponse order = orderService.create(new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(campaign.getProductId(), PURCHASE_QUANTITY))
        ));
        return new FlashSalePurchaseResponse(campaign.getId(), campaign.getProductId(), order.id(), "ACCEPTED");
    }
}

package com.nchuy099.ecommerce.order.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.FlashSalePurchaseResponse;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FlashSaleService {
    private static final int PURCHASE_QUANTITY = 1;

    private final FlashSaleCampaignRepository campaignRepository;
    private final FlashSalePurchaseGate purchaseGate;
    private final OrderService orderService;
    private final Clock clock;
    private final FlashSaleDistributedLock distributedLock;

    // Keeps focused unit tests independent from Redis while Spring uses the distributed lock bean.
    public FlashSaleService(
            FlashSaleCampaignRepository campaignRepository,
            FlashSalePurchaseGate purchaseGate,
            OrderService orderService,
            Clock clock
    ) {
        this(campaignRepository, purchaseGate, orderService, clock, NoopFlashSaleDistributedLock.INSTANCE);
    }

    public FlashSalePurchaseResponse purchase(Long campaignId, Long userId) {
        String lockToken = distributedLock.tryLock(campaignId);
        if (lockToken == null) {
            throw flashSaleRejected("LOCK_BUSY", "Flash sale is busy; please retry");
        }
        try {
            return purchaseUnderLock(campaignId, userId);
        } finally {
            distributedLock.unlock(campaignId, lockToken);
        }
    }

    private FlashSalePurchaseResponse purchaseUnderLock(Long campaignId, Long userId) {
        FlashSaleCampaignEntity campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> BusinessException.notFound(
                        "https://errors.ecom.local/flash-sale-campaign-not-found",
                        "Flash sale campaign not found",
                        "Flash sale campaign not found: " + campaignId
                ));

        Instant now = clock.instant();
        if (!campaign.isActiveAt(now)) {
            throw flashSaleRejected("NOT_ACTIVE", "Flash sale campaign is not active");
        }

        FlashSalePurchaseResult result = purchaseGate.tryPurchase(campaign, userId, PURCHASE_QUANTITY);
        if (result == FlashSalePurchaseResult.SOLD_OUT) {
            throw flashSaleRejected("SOLD_OUT", "Flash sale campaign is sold out");
        }
        if (result == FlashSalePurchaseResult.ALREADY_PURCHASED) {
            throw flashSaleRejected("ALREADY_PURCHASED", "Flash sale purchase limit already reached");
        }

        OrderResponse order = orderService.create(new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(campaign.getProductId(), PURCHASE_QUANTITY))
        ));
        return new FlashSalePurchaseResponse(campaign.getId(), campaign.getProductId(), order.id(), "ACCEPTED");
    }

    private static BusinessException flashSaleRejected(String reason, String message) {
        return BusinessException.conflict(
                        "https://errors.ecom.local/flash-sale-" + reason.toLowerCase().replace('_', '-'),
                        switch (reason) {
                            case "NOT_ACTIVE" -> "Flash sale not active";
                            case "SOLD_OUT" -> "SOLD_OUT";
                            case "ALREADY_PURCHASED" -> "ALREADY_PURCHASED";
                            case "LOCK_BUSY" -> "FLASH_SALE_BUSY";
                            default -> "Flash sale purchase rejected";
                        },
                        message
                )
                .withProperty("reason", reason);
    }
}

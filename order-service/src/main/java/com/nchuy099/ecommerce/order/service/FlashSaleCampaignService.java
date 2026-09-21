package com.nchuy099.ecommerce.order.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;

import com.nchuy099.ecommerce.order.dto.CreateFlashSaleCampaignRequest;
import com.nchuy099.ecommerce.order.dto.FlashSaleCampaignResponse;
import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignRecipientEntity;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRecipientRepository;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlashSaleCampaignService {
    private final FlashSaleCampaignRepository campaignRepository;
    private final FlashSaleCampaignRecipientRepository recipientRepository;
    private final Clock clock;

    public FlashSaleCampaignService(
            FlashSaleCampaignRepository campaignRepository,
            FlashSaleCampaignRecipientRepository recipientRepository,
            Clock clock
    ) {
        this.campaignRepository = campaignRepository;
        this.recipientRepository = recipientRepository;
        this.clock = clock;
    }

    @Transactional
    public FlashSaleCampaignResponse create(CreateFlashSaleCampaignRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw BusinessException.badRequest(
                    "https://errors.ecom.local/invalid-flash-sale-window",
                    "Invalid flash-sale window",
                    "endsAt must be after startsAt"
            );
        }
        if (new HashSet<>(request.recipientUserIds()).size() != request.recipientUserIds().size()) {
            throw BusinessException.badRequest(
                    "https://errors.ecom.local/duplicate-flash-sale-recipient",
                    "Duplicate flash-sale recipient",
                    "recipientUserIds must not contain duplicates"
            );
        }

        FlashSaleCampaignEntity campaign = campaignRepository.saveAndFlush(new FlashSaleCampaignEntity(
                request.productId(), request.stock(), request.startsAt(), request.endsAt(),
                request.promoPrice(), request.maxPerUser(), request.subject(), request.message()
        ));
        recipientRepository.saveAll(request.recipientUserIds().stream()
                .map(userId -> new FlashSaleCampaignRecipientEntity(campaign.getId(), userId))
                .toList());

        return FlashSaleCampaignResponse.of(
                campaign,
                request.recipientUserIds().size(),
                request.startsAt().minusSeconds(900)
        );
    }

    public Instant now() {
        return clock.instant();
    }
}

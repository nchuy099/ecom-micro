package com.nchuy099.ecommerce.order.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignRecipientEntity;
import com.nchuy099.ecommerce.order.entity.FlashSaleRecipientStatus;
import com.nchuy099.ecommerce.order.event.NotificationChannel;
import com.nchuy099.ecommerce.order.event.NotificationTask;
import com.nchuy099.ecommerce.order.notification.FlashSaleNotificationPublisher;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRecipientRepository;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlashSaleNotificationService {
    private final FlashSaleCampaignRepository campaignRepository;
    private final FlashSaleCampaignRecipientRepository recipientRepository;
    private final FlashSaleNotificationPublisher publisher;
    private final FlashSaleDistributedLock distributedLock;
    private final Clock clock;
    private final EntityManager entityManager;
    private final Duration leadTime;
    private final int batchSize;

    @Autowired
    public FlashSaleNotificationService(
            FlashSaleCampaignRepository campaignRepository,
            FlashSaleCampaignRecipientRepository recipientRepository,
            FlashSaleNotificationPublisher publisher,
            FlashSaleDistributedLock distributedLock,
            Clock clock,
            EntityManager entityManager,
            @Value("${flash-sale.notification.lead-time:15m}") Duration leadTime,
            @Value("${flash-sale.notification.batch-size:1000}") int batchSize
    ) {
        this.campaignRepository = campaignRepository;
        this.recipientRepository = recipientRepository;
        this.publisher = publisher;
        this.distributedLock = distributedLock;
        this.clock = clock;
        this.entityManager = entityManager;
        this.leadTime = leadTime;
        this.batchSize = Math.max(1, batchSize);
    }

    // Keeps focused unit tests independent from a JPA EntityManager.
    public FlashSaleNotificationService(
            FlashSaleCampaignRepository campaignRepository,
            FlashSaleCampaignRecipientRepository recipientRepository,
            FlashSaleNotificationPublisher publisher,
            FlashSaleDistributedLock distributedLock,
            Clock clock,
            Duration leadTime,
            int batchSize
    ) {
        this(campaignRepository, recipientRepository, publisher, distributedLock, clock,
                null, leadTime, batchSize);
    }

    @Transactional
    public void publishDueCampaigns() {
        if (!publisher.isEnabled()) {
            return;
        }
        Instant now = clock.instant();
        List<FlashSaleCampaignEntity> campaigns = campaignRepository.findByStartsAtBetween(
                now, now.plus(leadTime).plusSeconds(60)
        );
        for (FlashSaleCampaignEntity campaign : campaigns) {
            if (now.isBefore(campaign.getStartsAt().minus(leadTime))) {
                continue;
            }
            publishCampaign(campaign, now);
        }
    }

    private void publishCampaign(FlashSaleCampaignEntity campaign, Instant now) {
        String token = distributedLock.tryLock(campaign.getId());
        if (token == null) {
            return;
        }
        try {
            publishPendingRecipients(campaign, now);
        } finally {
            distributedLock.unlock(campaign.getId(), token);
        }
    }

    private void publishPendingRecipients(FlashSaleCampaignEntity campaign, Instant now) {
        while (true) {
            List<FlashSaleCampaignRecipientEntity> recipients = recipientRepository
                    .findByCampaignIdAndStatusOrderByIdAsc(
                            campaign.getId(), FlashSaleRecipientStatus.PENDING,
                            PageRequest.of(0, batchSize)
                    );
            if (recipients.isEmpty()) {
                return;
            }
            for (FlashSaleCampaignRecipientEntity recipient : recipients) {
                NotificationTask task = NotificationTask.flashSale(
                        campaign.getId(),
                        recipient.getUserId(),
                        NotificationChannel.PUSH,
                        campaign.getNotificationSubject() != null
                                ? campaign.getNotificationSubject()
                                : "Flash sale starts soon",
                        campaign.getNotificationMessage() != null
                                ? campaign.getNotificationMessage()
                                : "Flash sale for product " + campaign.getProductId()
                                        + " starts at " + campaign.getStartsAt()
                                        + " with price " + campaign.getPromoPrice() + ".",
                        0,
                        campaign.getStartsAt().minus(leadTime)
                );
                publisher.publish(task);
                recipient.markPublished(now);
                recipientRepository.saveAndFlush(recipient);
            }
            if (entityManager != null) {
                entityManager.clear();
            }
        }
    }
}

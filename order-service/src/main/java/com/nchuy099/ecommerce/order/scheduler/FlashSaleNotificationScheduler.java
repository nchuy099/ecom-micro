package com.nchuy099.ecommerce.order.scheduler;

import com.nchuy099.ecommerce.order.service.FlashSaleNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlashSaleNotificationScheduler {
    private final FlashSaleNotificationService notificationService;

    @Scheduled(fixedDelayString = "${flash-sale.notification.poll-interval-ms:30000}")
    public void publishDueCampaignNotifications() {
        notificationService.publishDueCampaigns();
    }
}

package com.nchuy099.ecommerce.order.scheduler;

import com.nchuy099.ecommerce.order.service.FlashSaleNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FlashSaleNotificationScheduler {
    private final FlashSaleNotificationService notificationService;

    public FlashSaleNotificationScheduler(FlashSaleNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${flash-sale.notification.poll-interval-ms:30000}")
    public void publishDueCampaignNotifications() {
        notificationService.publishDueCampaigns();
    }
}

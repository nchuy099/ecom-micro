package com.nchuy099.ecommerce.notification.listener;

import com.nchuy099.ecommerce.common.event.KafkaTopics;
import com.nchuy099.ecommerce.common.event.NotificationRequestedEvent;
import com.nchuy099.ecommerce.notification.service.NotificationFanOutService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationRequestListener {
    private final NotificationFanOutService fanOutService;

    public NotificationRequestListener(NotificationFanOutService fanOutService) {
        this.fanOutService = fanOutService;
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_REQUESTED, groupId = "notification-group")
    public void handleNotificationRequested(NotificationRequestedEvent event) {
        fanOutService.fanOut(event);
    }
}

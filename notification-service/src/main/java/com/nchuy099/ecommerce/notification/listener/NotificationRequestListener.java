package com.nchuy099.ecommerce.notification.listener;

import com.nchuy099.ecommerce.notification.event.KafkaTopics;
import com.nchuy099.ecommerce.notification.event.NotificationRequestedEvent;
import com.nchuy099.ecommerce.notification.service.NotificationFanOutService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationRequestListener {
    private final NotificationFanOutService fanOutService;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_REQUESTED, groupId = "notification-group")
    public void handleNotificationRequested(NotificationRequestedEvent event) {
        fanOutService.fanOut(event);
    }
}

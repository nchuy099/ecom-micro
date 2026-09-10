package com.nchuy099.ecommerce.notification.listener;

import com.nchuy099.ecommerce.common.event.KafkaTopics;
import com.nchuy099.ecommerce.common.event.NotificationRequestedEvent;
import com.nchuy099.ecommerce.common.event.OrderCreatedEvent;
import com.nchuy099.ecommerce.notification.service.NotificationFanOutService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private final NotificationFanOutService fanOutService;

    public OrderEventListener(NotificationFanOutService fanOutService) {
        this.fanOutService = fanOutService;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "notification-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (event == null) {
            return;
        }
        fanOutService.fanOut(NotificationRequestedEvent.forOrderCreated(event));
    }
}

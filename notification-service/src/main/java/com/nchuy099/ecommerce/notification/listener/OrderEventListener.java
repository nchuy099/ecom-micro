package com.nchuy099.ecommerce.notification.listener;

import com.nchuy099.ecommerce.notification.event.NotificationChannel;
import com.nchuy099.ecommerce.notification.event.OrderConfirmedEvent;
import com.nchuy099.ecommerce.notification.event.NotificationTask;
import com.nchuy099.ecommerce.notification.event.KafkaTopics;
import com.nchuy099.ecommerce.notification.publisher.NotificationTaskPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final NotificationTaskPublisher taskPublisher;

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "notification-order-events")
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        if (event == null) {
            return;
        }
        for (NotificationChannel channel : NotificationChannel.values()) {
            taskPublisher.publish(NotificationTask.orderConfirmed(event, channel));
        }
    }
}

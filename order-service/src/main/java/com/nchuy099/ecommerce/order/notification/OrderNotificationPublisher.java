package com.nchuy099.ecommerce.order.notification;

import com.nchuy099.ecommerce.order.event.KafkaTopics;
import com.nchuy099.ecommerce.order.event.OrderConfirmedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;

    public OrderNotificationPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${notification.publisher.enabled:true}") boolean enabled
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
    }

    public void publishOrderConfirmed(Long orderId, Long userId, String orderNumber, String message) {
        if (!enabled) {
            return;
        }
        OrderConfirmedEvent event = OrderConfirmedEvent.of(
                orderId,
                userId,
                orderNumber,
                message
        );
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, String.valueOf(orderId), event);
    }

    public static OrderNotificationPublisher noop() {
        return new OrderNotificationPublisher(null, false) {
            @Override
            public void publishOrderConfirmed(Long orderId, Long userId, String orderNumber, String message) {
                // Used by focused unit tests that do not start Kafka.
            }
        };
    }
}

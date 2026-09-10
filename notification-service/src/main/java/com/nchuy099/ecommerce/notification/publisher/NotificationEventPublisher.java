package com.nchuy099.ecommerce.notification.publisher;

import com.nchuy099.ecommerce.common.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.common.event.NotificationDlqEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishChannelRequest(NotificationChannelRequestedEvent event) {
        kafkaTemplate.send(event.eventType(), key(event.campaignId(), event.userId()), event);
    }

    public void publishDlq(NotificationDlqEvent event) {
        kafkaTemplate.send(event.eventType(), key(event.campaignId(), event.userId()), event);
    }

    private static String key(String campaignId, Long userId) {
        return campaignId + ":" + userId;
    }
}

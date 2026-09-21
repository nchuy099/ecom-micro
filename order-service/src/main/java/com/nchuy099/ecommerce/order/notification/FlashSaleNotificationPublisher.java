package com.nchuy099.ecommerce.order.notification;

import java.util.concurrent.ExecutionException;

import com.nchuy099.ecommerce.order.event.KafkaTopics;
import com.nchuy099.ecommerce.order.event.NotificationTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FlashSaleNotificationPublisher {
    private static final int MAX_PRIORITY_PARTITION = 11;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;

    public FlashSaleNotificationPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${notification.publisher.enabled:true}") boolean enabled
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
    }

    public void publish(NotificationTask task) {
        if (!enabled) {
            return;
        }
        int partition = Math.max(0, Math.min(MAX_PRIORITY_PARTITION, task.priority()));
        try {
            kafkaTemplate.send(
                    KafkaTopics.NOTIFICATION_TASKS,
                    partition,
                    task.idempotencyKey(),
                    task
            ).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Flash-sale notification publish interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Flash-sale notification publish failed", ex.getCause());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}

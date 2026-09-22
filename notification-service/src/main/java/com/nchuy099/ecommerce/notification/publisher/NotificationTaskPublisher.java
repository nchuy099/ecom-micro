package com.nchuy099.ecommerce.notification.publisher;

import com.nchuy099.ecommerce.notification.event.KafkaTopics;
import com.nchuy099.ecommerce.notification.event.NotificationTask;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationTaskPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(NotificationTask task) {
        kafkaTemplate.send(
                KafkaTopics.NOTIFICATION_TASKS,
                partition(task.priority()),
                task.idempotencyKey(),
                task
        );
    }

    public void publishRetry(NotificationTask task) {
        kafkaTemplate.send(
                KafkaTopics.NOTIFICATION_RETRY,
                partition(task.priority()),
                task.idempotencyKey(),
                task
        );
    }

    public void publishDlq(NotificationTask task) {
        kafkaTemplate.send(
                KafkaTopics.NOTIFICATION_DLQ,
                partition(task.priority()),
                task.idempotencyKey(),
                task
        );
    }

    private static int partition(int priority) {
        return Math.max(0, Math.min(11, priority));
    }
}

package com.nchuy099.ecommerce.notification.listener;

import com.nchuy099.ecommerce.notification.event.KafkaTopics;
import com.nchuy099.ecommerce.notification.event.NotificationTask;
import com.nchuy099.ecommerce.notification.service.NotificationTaskWorkerService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationTaskListener {
    private final NotificationTaskWorkerService workerService;

    @KafkaListener(
            topics = {KafkaTopics.NOTIFICATION_TASKS, KafkaTopics.NOTIFICATION_RETRY},
            groupId = "notification-task-worker"
    )
    public void handle(NotificationTask task) {
        workerService.process(task);
    }
}

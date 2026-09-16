package com.nchuy099.ecommerce.notification.listener;

import com.nchuy099.ecommerce.notification.event.KafkaTopics;
import com.nchuy099.ecommerce.notification.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.notification.service.NotificationChannelWorkerService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationChannelListener {
    private final NotificationChannelWorkerService workerService;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EMAIL_REQUESTED, groupId = "notification-email-worker")
    public void handleEmail(NotificationChannelRequestedEvent event) {
        workerService.process(event);
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_PUSH_REQUESTED, groupId = "notification-push-worker")
    public void handlePush(NotificationChannelRequestedEvent event) {
        workerService.process(event);
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_SMS_REQUESTED, groupId = "notification-sms-worker")
    public void handleSms(NotificationChannelRequestedEvent event) {
        workerService.process(event);
    }
}

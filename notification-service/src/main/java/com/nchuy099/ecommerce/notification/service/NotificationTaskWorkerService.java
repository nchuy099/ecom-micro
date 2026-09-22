package com.nchuy099.ecommerce.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.notification.config.NotificationProperties;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryEntity;
import com.nchuy099.ecommerce.notification.entity.NotificationDlqEntity;
import com.nchuy099.ecommerce.notification.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.notification.event.NotificationChannel;
import com.nchuy099.ecommerce.notification.event.NotificationDlqEvent;
import com.nchuy099.ecommerce.notification.event.NotificationTask;
import com.nchuy099.ecommerce.notification.provider.NotificationProvider;
import com.nchuy099.ecommerce.notification.provider.PushRateLimiter;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.publisher.NotificationTaskPublisher;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import com.nchuy099.ecommerce.notification.repository.NotificationDlqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationTaskWorkerService {
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDlqRepository dlqRepository;
    private final NotificationProvider provider;
    private final NotificationTaskPublisher taskPublisher;
    private final NotificationEventPublisher legacyPublisher;
    private final NotificationProperties properties;
    private final PushRateLimiter pushRateLimiter;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(NotificationTask task) {
        if (task == null) {
            return;
        }
        NotificationDeliveryEntity delivery = deliveryRepository
                .findByCampaignIdAndUserIdAndChannel(task.campaignId(), task.userId(), task.channel())
                .orElseGet(() -> deliveryRepository.saveAndFlush(new NotificationDeliveryEntity(
                        task.campaignId(), task.userId(), task.channel(), task.subject(), task.message()
                )));
        if (delivery.isSent()) {
            return;
        }

        try {
            if (task.channel() == NotificationChannel.PUSH) {
                pushRateLimiter.acquire();
            }
            provider.send(toProviderEvent(task));
            delivery.markSent(task.attempt() + 1);
            deliveryRepository.saveAndFlush(delivery);
        } catch (RuntimeException ex) {
            int nextAttempt = task.attempt() + 1;
            if (nextAttempt < Math.max(1, properties.getWorker().getMaxAttempts())) {
                taskPublisher.publishRetry(task.retry(nextAttempt));
                return;
            }
            String reason = ex.getMessage() == null ? "Notification provider failure" : ex.getMessage();
            delivery.markFailed(nextAttempt, reason);
            deliveryRepository.saveAndFlush(delivery);
            NotificationDlqEntity dlq = dlqRepository.saveAndFlush(new NotificationDlqEntity(
                    task.campaignId(), task.userId(), task.channel(), task.subject(), task.message(),
                    toJson(task), reason, nextAttempt
            ));
            taskPublisher.publishDlq(task.retry(nextAttempt));
            legacyPublisher.publishDlq(NotificationDlqEvent.of(
                    dlq.getCampaignId(), dlq.getUserId(), dlq.getChannel(), dlq.getFailureReason(), dlq.getAttempts()
            ));
        }
    }

    private NotificationChannelRequestedEvent toProviderEvent(NotificationTask task) {
        return new NotificationChannelRequestedEvent(
                task.taskId(),
                NotificationChannelRequestedEvent.topicFor(task.channel()),
                task.campaignId(), task.userId(), task.channel(), task.subject(), task.message(), task.scheduledAt()
        );
    }

    private String toJson(NotificationTask task) {
        try {
            return objectMapper.writeValueAsString(task);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize notification task", ex);
        }
    }
}

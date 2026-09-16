package com.nchuy099.ecommerce.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.notification.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.notification.event.NotificationDlqEvent;
import com.nchuy099.ecommerce.notification.config.NotificationProperties;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryEntity;
import com.nchuy099.ecommerce.notification.entity.NotificationDlqEntity;
import com.nchuy099.ecommerce.notification.provider.NotificationProvider;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import com.nchuy099.ecommerce.notification.repository.NotificationDlqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationChannelWorkerService {
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDlqRepository dlqRepository;
    private final NotificationProvider provider;
    private final NotificationEventPublisher publisher;
    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(NotificationChannelRequestedEvent event) {
        if (event == null) {
            return;
        }
        NotificationDeliveryEntity delivery = deliveryRepository
                .findByCampaignIdAndUserIdAndChannel(event.campaignId(), event.userId(), event.channel())
                .orElseGet(() -> deliveryRepository.saveAndFlush(new NotificationDeliveryEntity(
                        event.campaignId(),
                        event.userId(),
                        event.channel(),
                        event.subject(),
                        event.message()
                )));
        if (delivery.isSent()) {
            return;
        }

        int maxAttempts = Math.max(1, properties.getWorker().getMaxAttempts());
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                provider.send(event);
                delivery.markSent(attempt);
                deliveryRepository.saveAndFlush(delivery);
                return;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                backoffBeforeRetry(attempt, maxAttempts);
            }
        }

        String failureReason = lastFailure != null ? lastFailure.getMessage() : "Unknown notification provider failure";
        delivery.markFailed(maxAttempts, failureReason);
        deliveryRepository.saveAndFlush(delivery);
        NotificationDlqEntity dlq = dlqRepository.saveAndFlush(new NotificationDlqEntity(
                event.campaignId(),
                event.userId(),
                event.channel(),
                event.subject(),
                event.message(),
                toJson(event),
                failureReason,
                maxAttempts
        ));
        publisher.publishDlq(NotificationDlqEvent.of(
                dlq.getCampaignId(),
                dlq.getUserId(),
                dlq.getChannel(),
                dlq.getFailureReason(),
                dlq.getAttempts()
        ));
    }

    private void backoffBeforeRetry(int attempt, int maxAttempts) {
        if (attempt >= maxAttempts || properties.getWorker().getBackoff().isZero()) {
            return;
        }
        try {
            Thread.sleep(properties.getWorker().getBackoff().toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Notification retry interrupted", ex);
        }
    }

    private String toJson(NotificationChannelRequestedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize notification payload", ex);
        }
    }
}

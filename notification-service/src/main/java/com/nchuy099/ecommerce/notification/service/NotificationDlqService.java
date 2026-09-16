package com.nchuy099.ecommerce.notification.service;

import java.util.List;

import com.nchuy099.ecommerce.notification.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.notification.dto.NotificationDlqResponse;
import com.nchuy099.ecommerce.notification.dto.NotificationReplayResponse;
import com.nchuy099.ecommerce.notification.entity.NotificationDlqEntity;
import com.nchuy099.ecommerce.notification.exception.BusinessException;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import com.nchuy099.ecommerce.notification.repository.NotificationDlqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationDlqService {
    private final NotificationDlqRepository dlqRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationEventPublisher publisher;

    @Transactional(readOnly = true)
    public List<NotificationDlqResponse> findAll() {
        return dlqRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationDlqResponse::from)
                .toList();
    }

    @Transactional
    public NotificationReplayResponse replay(Long id) {
        NotificationDlqEntity dlq = dlqRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "https://errors.ecom.local/notification-dlq-not-found",
                        "Notification DLQ entry not found",
                        "Notification DLQ entry not found: " + id
                ));
        boolean alreadySent = deliveryRepository
                .findByCampaignIdAndUserIdAndChannel(dlq.getCampaignId(), dlq.getUserId(), dlq.getChannel())
                .map(delivery -> delivery.isSent())
                .orElse(false);
        if (!alreadySent) {
            dlq.markReplayed();
            dlqRepository.saveAndFlush(dlq);
            publisher.publishChannelRequest(new NotificationChannelRequestedEvent(
                    java.util.UUID.randomUUID(),
                    NotificationChannelRequestedEvent.topicFor(dlq.getChannel()),
                    dlq.getCampaignId(),
                    dlq.getUserId(),
                    dlq.getChannel(),
                    dlq.getSubject(),
                    dlq.getMessage(),
                    java.time.Instant.now()
            ));
        }
        return new NotificationReplayResponse(dlq.getId(), dlq.getStatus().name(), alreadySent);
    }
}

package com.nchuy099.ecommerce.notification.service;

import java.util.List;

import com.nchuy099.ecommerce.notification.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.notification.dto.NotificationDlqResponse;
import com.nchuy099.ecommerce.notification.dto.NotificationReplayResponse;
import com.nchuy099.ecommerce.notification.entity.NotificationDlqEntity;
import com.nchuy099.ecommerce.notification.exception.BusinessException;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.publisher.NotificationTaskPublisher;
import com.nchuy099.ecommerce.notification.event.NotificationTask;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import com.nchuy099.ecommerce.notification.repository.NotificationDlqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NotificationDlqService {
    private final NotificationDlqRepository dlqRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationEventPublisher publisher;
    private final NotificationTaskPublisher taskPublisher;

    // Keeps focused legacy tests and callers independent from the task publisher.
    public NotificationDlqService(
            NotificationDlqRepository dlqRepository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationEventPublisher publisher
    ) {
        this(dlqRepository, deliveryRepository, publisher, null);
    }

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
            if (taskPublisher != null
                    && (dlq.getCampaignId().startsWith("order:") || dlq.getCampaignId().startsWith("flash-sale:"))) {
                taskPublisher.publish(new NotificationTask(
                        java.util.UUID.randomUUID(),
                        "NOTIFICATION_TASK",
                        "DLQ_REPLAY",
                        dlq.getCampaignId() + ":" + dlq.getUserId() + ":" + dlq.getChannel(),
                        dlq.getCampaignId(),
                        dlq.getUserId(),
                        dlq.getChannel(),
                        dlq.getSubject(),
                        dlq.getMessage(),
                        0,
                        0,
                        java.time.Instant.now()
                ));
            } else {
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
        }
        return new NotificationReplayResponse(dlq.getId(), dlq.getStatus().name(), alreadySent);
    }
}

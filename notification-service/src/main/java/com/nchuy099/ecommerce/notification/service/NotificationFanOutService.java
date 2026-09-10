package com.nchuy099.ecommerce.notification.service;

import java.util.Arrays;

import com.nchuy099.ecommerce.common.event.NotificationChannel;
import com.nchuy099.ecommerce.common.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.common.event.NotificationRequestedEvent;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryEntity;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationFanOutService {
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationEventPublisher publisher;

    public NotificationFanOutService(NotificationDeliveryRepository deliveryRepository, NotificationEventPublisher publisher) {
        this.deliveryRepository = deliveryRepository;
        this.publisher = publisher;
    }

    @Transactional
    public void fanOut(NotificationRequestedEvent event) {
        if (event == null) {
            return;
        }
        Arrays.stream(NotificationChannel.values())
                .forEach(channel -> enqueueChannel(event, channel));
    }

    private void enqueueChannel(NotificationRequestedEvent event, NotificationChannel channel) {
        if (deliveryRepository.existsByCampaignIdAndUserIdAndChannel(event.campaignId(), event.userId(), channel)) {
            return;
        }
        try {
            deliveryRepository.saveAndFlush(new NotificationDeliveryEntity(
                    event.campaignId(),
                    event.userId(),
                    channel,
                    event.subject(),
                    event.message()
            ));
        } catch (DataIntegrityViolationException ignored) {
            return;
        }
        publisher.publishChannelRequest(NotificationChannelRequestedEvent.of(event, channel));
    }
}

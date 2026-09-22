package com.nchuy099.ecommerce.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.notification.config.NotificationProperties;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryEntity;
import com.nchuy099.ecommerce.notification.event.NotificationChannel;
import com.nchuy099.ecommerce.notification.event.NotificationTask;
import com.nchuy099.ecommerce.notification.provider.NotificationProvider;
import com.nchuy099.ecommerce.notification.provider.PushRateLimiter;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.publisher.NotificationTaskPublisher;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import com.nchuy099.ecommerce.notification.repository.NotificationDlqRepository;
import com.nchuy099.ecommerce.notification.service.NotificationTaskWorkerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationTaskWorkerServiceTest {
    @Mock private NotificationDeliveryRepository deliveryRepository;
    @Mock private NotificationDlqRepository dlqRepository;
    @Mock private NotificationProvider provider;
    @Mock private NotificationTaskPublisher taskPublisher;
    @Mock private NotificationEventPublisher legacyPublisher;
    @Mock private PushRateLimiter rateLimiter;

    @Test
    void sendsTaskAndMarksDeliverySent() {
        NotificationTask task = new NotificationTask(
                UUID.randomUUID(), "NOTIFICATION_TASK", "FLASH_SALE_CAMPAIGN",
                "flash-sale:7:42:PUSH", "flash-sale:7", 42L, NotificationChannel.PUSH,
                "Sale", "Starts soon", 0, 0, Instant.now()
        );
        NotificationDeliveryEntity delivery = new NotificationDeliveryEntity(
                task.campaignId(), task.userId(), task.channel(), task.subject(), task.message()
        );
        when(deliveryRepository.findByCampaignIdAndUserIdAndChannel(
                task.campaignId(), task.userId(), task.channel()
        )).thenReturn(Optional.of(delivery));
        when(deliveryRepository.saveAndFlush(any(NotificationDeliveryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationProperties properties = new NotificationProperties();
        properties.getWorker().setMaxAttempts(3);
        NotificationTaskWorkerService service = new NotificationTaskWorkerService(
                deliveryRepository, dlqRepository, provider, taskPublisher, legacyPublisher,
                properties, rateLimiter, new ObjectMapper().findAndRegisterModules()
        );

        service.process(task);

        verify(rateLimiter).acquire();
        verify(provider).send(any());
        verify(deliveryRepository).saveAndFlush(delivery);
    }
}

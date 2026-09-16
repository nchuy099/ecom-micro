package com.nchuy099.ecommerce.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.notification.event.NotificationChannel;
import com.nchuy099.ecommerce.notification.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.notification.event.NotificationRequestedEvent;
import com.nchuy099.ecommerce.notification.config.NotificationProperties;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryEntity;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryStatus;
import com.nchuy099.ecommerce.notification.entity.NotificationDlqEntity;
import com.nchuy099.ecommerce.notification.provider.NotificationProvider;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import com.nchuy099.ecommerce.notification.repository.NotificationDlqRepository;
import com.nchuy099.ecommerce.notification.service.NotificationChannelWorkerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationChannelWorkerServiceTest {
    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private NotificationDlqRepository dlqRepository;

    @Mock
    private NotificationProvider provider;

    @Mock
    private NotificationEventPublisher publisher;

    @Test
    void successfulSendMarksDeliverySent() {
        NotificationChannelRequestedEvent event = event(NotificationChannel.EMAIL);
        NotificationDeliveryEntity delivery = delivery(event);
        when(deliveryRepository.findByCampaignIdAndUserIdAndChannel("campaign-1", 100L, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(delivery));
        when(deliveryRepository.saveAndFlush(any(NotificationDeliveryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().process(event);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        verify(provider).send(event);
        verify(dlqRepository, never()).saveAndFlush(any());
    }

    @Test
    void repeatedProviderFailureWritesDlqAfterFinalAttempt() {
        NotificationChannelRequestedEvent event = event(NotificationChannel.SMS);
        NotificationDeliveryEntity delivery = delivery(event);
        when(deliveryRepository.findByCampaignIdAndUserIdAndChannel("campaign-1", 100L, NotificationChannel.SMS))
                .thenReturn(Optional.of(delivery));
        when(deliveryRepository.saveAndFlush(any(NotificationDeliveryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dlqRepository.saveAndFlush(any(NotificationDlqEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new IllegalStateException("provider down")).when(provider).send(event);

        service().process(event);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(delivery.getAttempts()).isEqualTo(3);
        verify(provider, times(3)).send(event);
        ArgumentCaptor<NotificationDlqEntity> dlq = ArgumentCaptor.forClass(NotificationDlqEntity.class);
        verify(dlqRepository).saveAndFlush(dlq.capture());
        assertThat(dlq.getValue().getChannel()).isEqualTo(NotificationChannel.SMS);
        assertThat(dlq.getValue().getFailureReason()).isEqualTo("provider down");
        verify(publisher).publishDlq(any());
    }

    @Test
    void alreadySentDeliveryIsNotSentAgain() {
        NotificationChannelRequestedEvent event = event(NotificationChannel.PUSH);
        NotificationDeliveryEntity delivery = delivery(event);
        delivery.markSent(1);
        when(deliveryRepository.findByCampaignIdAndUserIdAndChannel("campaign-1", 100L, NotificationChannel.PUSH))
                .thenReturn(Optional.of(delivery));

        service().process(event);

        verify(provider, never()).send(any());
        verify(dlqRepository, never()).saveAndFlush(any());
    }

    private NotificationChannelWorkerService service() {
        NotificationProperties properties = new NotificationProperties();
        properties.getWorker().setMaxAttempts(3);
        properties.getWorker().setBackoff(Duration.ZERO);
        return new NotificationChannelWorkerService(
                deliveryRepository,
                dlqRepository,
                provider,
                publisher,
                properties,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    private static NotificationChannelRequestedEvent event(NotificationChannel channel) {
        return NotificationChannelRequestedEvent.of(
                NotificationRequestedEvent.of("campaign-1", 100L, "Subject", "Message"),
                channel
        );
    }

    private static NotificationDeliveryEntity delivery(NotificationChannelRequestedEvent event) {
        return new NotificationDeliveryEntity(
                event.campaignId(),
                event.userId(),
                event.channel(),
                event.subject(),
                event.message()
        );
    }
}

package com.nchuy099.ecommerce.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nchuy099.ecommerce.common.event.NotificationChannel;
import com.nchuy099.ecommerce.common.event.NotificationRequestedEvent;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryEntity;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import com.nchuy099.ecommerce.notification.service.NotificationFanOutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationFanOutServiceTest {
    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private NotificationEventPublisher publisher;

    @Test
    void fansOutRequestToAllChannels() {
        NotificationRequestedEvent event = NotificationRequestedEvent.of("campaign-1", 100L, "Sale", "Body");
        when(deliveryRepository.existsByCampaignIdAndUserIdAndChannel(any(), any(), any())).thenReturn(false);
        when(deliveryRepository.saveAndFlush(any(NotificationDeliveryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new NotificationFanOutService(deliveryRepository, publisher).fanOut(event);

        verify(deliveryRepository, times(NotificationChannel.values().length)).saveAndFlush(any(NotificationDeliveryEntity.class));
        verify(publisher, times(NotificationChannel.values().length)).publishChannelRequest(any());
    }

    @Test
    void duplicateRequestDoesNotPublishAgain() {
        NotificationRequestedEvent event = NotificationRequestedEvent.of("campaign-1", 100L, "Sale", "Body");
        when(deliveryRepository.existsByCampaignIdAndUserIdAndChannel(any(), any(), any())).thenReturn(true);

        new NotificationFanOutService(deliveryRepository, publisher).fanOut(event);

        verify(deliveryRepository, never()).saveAndFlush(any());
        verify(publisher, never()).publishChannelRequest(any());
    }
}

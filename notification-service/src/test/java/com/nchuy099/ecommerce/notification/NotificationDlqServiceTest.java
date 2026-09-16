package com.nchuy099.ecommerce.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.nchuy099.ecommerce.notification.event.NotificationChannel;
import com.nchuy099.ecommerce.notification.dto.NotificationReplayResponse;
import com.nchuy099.ecommerce.notification.entity.NotificationDeliveryEntity;
import com.nchuy099.ecommerce.notification.entity.NotificationDlqEntity;
import com.nchuy099.ecommerce.notification.entity.NotificationDlqStatus;
import com.nchuy099.ecommerce.notification.exception.BusinessException;
import com.nchuy099.ecommerce.notification.publisher.NotificationEventPublisher;
import com.nchuy099.ecommerce.notification.repository.NotificationDeliveryRepository;
import com.nchuy099.ecommerce.notification.repository.NotificationDlqRepository;
import com.nchuy099.ecommerce.notification.service.NotificationDlqService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationDlqServiceTest {
    @Mock
    private NotificationDlqRepository dlqRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private NotificationEventPublisher publisher;

    @Test
    void listsDlqEntries() {
        NotificationDlqEntity dlq = dlq(1L);
        when(dlqRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(dlq));

        assertThat(service().findAll()).hasSize(1);
    }

    @Test
    void replayRequeuesOpenDlqItem() {
        NotificationDlqEntity dlq = dlq(1L);
        when(dlqRepository.findById(1L)).thenReturn(Optional.of(dlq));
        when(deliveryRepository.findByCampaignIdAndUserIdAndChannel("campaign-1", 100L, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());

        NotificationReplayResponse response = service().replay(1L);

        assertThat(response.dlqId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("REPLAYED");
        assertThat(response.alreadySent()).isFalse();
        assertThat(dlq.getStatus()).isEqualTo(NotificationDlqStatus.REPLAYED);
        verify(publisher).publishChannelRequest(any());
    }

    @Test
    void replayDoesNotDuplicateAlreadySuccessfulSend() {
        NotificationDlqEntity dlq = dlq(1L);
        NotificationDeliveryEntity delivery = new NotificationDeliveryEntity("campaign-1", 100L, NotificationChannel.EMAIL, "Subject", "Message");
        delivery.markSent(1);
        when(dlqRepository.findById(1L)).thenReturn(Optional.of(dlq));
        when(deliveryRepository.findByCampaignIdAndUserIdAndChannel("campaign-1", 100L, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(delivery));

        NotificationReplayResponse response = service().replay(1L);

        assertThat(response.alreadySent()).isTrue();
        verify(publisher, never()).publishChannelRequest(any());
    }

    @Test
    void missingDlqThrowsNotFound() {
        when(dlqRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().replay(404L))
                .isInstanceOf(BusinessException.class);
    }

    private NotificationDlqService service() {
        return new NotificationDlqService(dlqRepository, deliveryRepository, publisher);
    }

    private static NotificationDlqEntity dlq(Long id) {
        NotificationDlqEntity dlq = new NotificationDlqEntity(
                "campaign-1",
                100L,
                NotificationChannel.EMAIL,
                "Subject",
                "Message",
                "{}",
                "provider down",
                3
        );
        ReflectionTestUtils.setField(dlq, "id", id);
        return dlq;
    }
}

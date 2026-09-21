package com.nchuy099.ecommerce.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignRecipientEntity;
import com.nchuy099.ecommerce.order.entity.FlashSaleRecipientStatus;
import com.nchuy099.ecommerce.order.event.NotificationTask;
import com.nchuy099.ecommerce.order.notification.FlashSaleNotificationPublisher;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRecipientRepository;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRepository;
import com.nchuy099.ecommerce.order.service.FlashSaleCampaignService;
import com.nchuy099.ecommerce.order.service.FlashSaleDistributedLock;
import com.nchuy099.ecommerce.order.service.FlashSaleNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FlashSaleNotificationServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-26T10:00:00Z");

    @Mock
    private FlashSaleCampaignRepository campaignRepository;
    @Mock
    private FlashSaleCampaignRecipientRepository recipientRepository;
    @Mock
    private FlashSaleNotificationPublisher publisher;
    @Mock
    private FlashSaleDistributedLock distributedLock;

    @Test
    void publishesOnePushTaskPerPendingRecipientAndMarksItPublished() {
        FlashSaleCampaignEntity campaign = new FlashSaleCampaignEntity(
                10L, 100, NOW.plusSeconds(600), NOW.plusSeconds(3600), BigDecimal.TEN, 1,
                "Sale soon", "The sale starts soon"
        );
        ReflectionTestUtils.setField(campaign, "id", 7L);
        FlashSaleCampaignRecipientEntity recipient = new FlashSaleCampaignRecipientEntity(7L, 42L);

        when(publisher.isEnabled()).thenReturn(true);
        when(campaignRepository.findByStartsAtBetween(any(), any())).thenReturn(List.of(campaign));
        when(distributedLock.tryLock(7L)).thenReturn("lock");
        when(recipientRepository.findByCampaignIdAndStatusOrderByIdAsc(
                eq(7L), eq(FlashSaleRecipientStatus.PENDING), any(Pageable.class)
        )).thenReturn(List.of(recipient), List.of());

        FlashSaleNotificationService service = new FlashSaleNotificationService(
                campaignRepository,
                recipientRepository,
                publisher,
                distributedLock,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(15),
                100
        );

        service.publishDueCampaigns();

        verify(publisher).publish(any(NotificationTask.class));
        verify(recipientRepository).saveAndFlush(recipient);
        verify(distributedLock).unlock(7L, "lock");
    }
}

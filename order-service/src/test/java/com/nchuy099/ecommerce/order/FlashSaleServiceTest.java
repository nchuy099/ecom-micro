package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.FlashSalePurchaseResponse;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.repository.FlashSaleCampaignRepository;
import com.nchuy099.ecommerce.order.service.FlashSalePurchaseGate;
import com.nchuy099.ecommerce.order.service.FlashSalePurchaseResult;
import com.nchuy099.ecommerce.order.service.FlashSaleDistributedLock;
import com.nchuy099.ecommerce.order.service.FlashSaleService;
import com.nchuy099.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FlashSaleServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-05T10:00:00Z");

    @Mock
    private FlashSaleCampaignRepository campaignRepository;

    @Mock
    private FlashSalePurchaseGate purchaseGate;

    @Mock
    private OrderService orderService;

    @Mock
    private FlashSaleDistributedLock distributedLock;

    @Test
    void acceptedPurchaseCreatesOrderThroughExistingSagaPath() {
        FlashSaleCampaignEntity campaign = campaign(1L, 10L, 5, NOW.minusSeconds(60), NOW.plusSeconds(60), 1);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(purchaseGate.tryPurchase(campaign, 100L, 1)).thenReturn(FlashSalePurchaseResult.ACCEPTED);
        when(orderService.create(any())).thenReturn(orderResponse(20L, 100L));

        FlashSaleService service = service(purchaseGate);
        FlashSalePurchaseResponse response = service.purchase(1L, 100L);

        assertThat(response.campaignId()).isEqualTo(1L);
        assertThat(response.productId()).isEqualTo(10L);
        assertThat(response.orderId()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo("ACCEPTED");

        ArgumentCaptor<CreateOrderRequest> request = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(orderService).create(request.capture());
        assertThat(request.getValue().userId()).isEqualTo(100L);
        assertThat(request.getValue().items()).hasSize(1);
        assertThat(request.getValue().items().get(0).productId()).isEqualTo(10L);
        assertThat(request.getValue().items().get(0).quantity()).isEqualTo(1);
    }

    @Test
    void inactiveCampaignDoesNotRunLuaOrCreateOrder() {
        FlashSaleCampaignEntity campaign = campaign(1L, 10L, 5, NOW.plusSeconds(60), NOW.plusSeconds(120), 1);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        FlashSaleService service = service(purchaseGate);

        assertThatThrownBy(() -> service.purchase(1L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getProperties().get("reason")).isEqualTo("NOT_ACTIVE"));
        verify(purchaseGate, never()).tryPurchase(any(), any(), org.mockito.ArgumentMatchers.anyInt());
        verify(orderService, never()).create(any());
    }

    @Test
    void soldOutDoesNotCreateOrder() {
        FlashSaleCampaignEntity campaign = campaign(1L, 10L, 0, NOW.minusSeconds(60), NOW.plusSeconds(60), 1);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(purchaseGate.tryPurchase(campaign, 100L, 1)).thenReturn(FlashSalePurchaseResult.SOLD_OUT);

        FlashSaleService service = service(purchaseGate);

        assertThatThrownBy(() -> service.purchase(1L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getProperties().get("reason")).isEqualTo("SOLD_OUT"));
        verify(orderService, never()).create(any());
    }

    @Test
    void alreadyPurchasedDoesNotCreateOrder() {
        FlashSaleCampaignEntity campaign = campaign(1L, 10L, 5, NOW.minusSeconds(60), NOW.plusSeconds(60), 1);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(purchaseGate.tryPurchase(campaign, 100L, 1)).thenReturn(FlashSalePurchaseResult.ALREADY_PURCHASED);

        FlashSaleService service = service(purchaseGate);

        assertThatThrownBy(() -> service.purchase(1L, 100L))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getProperties().get("reason")).isEqualTo("ALREADY_PURCHASED"));
        verify(orderService, never()).create(any());
    }

    @Test
    void releasesDistributedLockWhenPurchaseIsRejected() {
        FlashSaleCampaignEntity campaign = campaign(1L, 10L, 0, NOW.minusSeconds(60), NOW.plusSeconds(60), 1);
        when(distributedLock.tryLock(1L)).thenReturn("lock-token");
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(purchaseGate.tryPurchase(campaign, 100L, 1)).thenReturn(FlashSalePurchaseResult.SOLD_OUT);

        FlashSaleService service = service(purchaseGate, distributedLock);

        assertThatThrownBy(() -> service.purchase(1L, 100L))
                .isInstanceOf(BusinessException.class);

        verify(distributedLock).unlock(1L, "lock-token");
    }

    @Test
    void concurrentPurchasesNeverCreateMoreOrdersThanCampaignStock() throws Exception {
        FlashSaleCampaignEntity campaign = campaign(1L, 10L, 3, NOW.minusSeconds(60), NOW.plusSeconds(60), 1);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        AtomicLong orderIds = new AtomicLong(100L);
        when(orderService.create(any())).thenAnswer(invocation -> orderResponse(orderIds.incrementAndGet(), 100L));

        AtomicLuaGate gate = new AtomicLuaGate(campaign.getStock(), campaign.getMaxPerUser());
        FlashSaleService service = service(gate);
        int attempts = 25;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(attempts);

        for (int i = 0; i < attempts; i++) {
            long userId = 100L + i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    service.purchase(1L, userId);
                    accepted.incrementAndGet();
                } catch (BusinessException ignored) {
                    // Rejections are expected after stock is exhausted.
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        assertThat(accepted.get()).isEqualTo(3);
        verify(orderService, times(3)).create(any());
    }

    private FlashSaleService service(FlashSalePurchaseGate gate) {
        return new FlashSaleService(campaignRepository, gate, orderService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private FlashSaleService service(FlashSalePurchaseGate gate, FlashSaleDistributedLock lock) {
        return new FlashSaleService(campaignRepository, gate, orderService, Clock.fixed(NOW, ZoneOffset.UTC), lock);
    }

    private static FlashSaleCampaignEntity campaign(Long id, Long productId, int stock, Instant startsAt, Instant endsAt, int maxPerUser) {
        FlashSaleCampaignEntity campaign = new FlashSaleCampaignEntity(
                productId,
                stock,
                startsAt,
                endsAt,
                new BigDecimal("49.99"),
                maxPerUser
        );
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }

    private static OrderResponse orderResponse(Long id, Long userId) {
        return new OrderResponse(id, "ORD-" + id, userId, OrderStatus.PENDING, BigDecimal.ZERO, List.of(), NOW, NOW);
    }

    private static final class AtomicLuaGate implements FlashSalePurchaseGate {
        private final AtomicInteger stock;
        private final int maxPerUser;
        private final ConcurrentHashMap<Long, AtomicInteger> userPurchases = new ConcurrentHashMap<>();

        private AtomicLuaGate(int stock, int maxPerUser) {
            this.stock = new AtomicInteger(stock);
            this.maxPerUser = maxPerUser;
        }

        @Override
        public synchronized FlashSalePurchaseResult tryPurchase(FlashSaleCampaignEntity campaign, Long userId, int quantity) {
            AtomicInteger purchased = userPurchases.computeIfAbsent(userId, ignored -> new AtomicInteger());
            if (purchased.get() + quantity > maxPerUser) {
                return FlashSalePurchaseResult.ALREADY_PURCHASED;
            }
            if (stock.get() < quantity) {
                return FlashSalePurchaseResult.SOLD_OUT;
            }
            stock.addAndGet(-quantity);
            purchased.addAndGet(quantity);
            return FlashSalePurchaseResult.ACCEPTED;
        }
    }
}

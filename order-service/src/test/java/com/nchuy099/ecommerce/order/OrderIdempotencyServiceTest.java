package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.common.ApiResponse;
import com.nchuy099.ecommerce.order.dto.OrderItemResponse;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.IdempotencyInProgressException;
import com.nchuy099.ecommerce.order.idempotency.OrderIdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class OrderIdempotencyServiceTest {
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);
    private static final Duration PROCESSING_TTL = Duration.ofSeconds(60);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private OrderIdempotencyService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new OrderIdempotencyService(redisTemplate, objectMapper, COMPLETED_TTL, PROCESSING_TTL);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void firstRequestReservesKeyProcessesAndStoresCompletedResponse() {
        when(valueOperations.setIfAbsent(eq("idem:order:create:100:abc"), org.mockito.ArgumentMatchers.anyString(), eq(PROCESSING_TTL)))
                .thenReturn(true);
        AtomicInteger calls = new AtomicInteger();

        ApiResponse<OrderResponse> response = service.executeCreateOrder("100", "abc", () -> {
            calls.incrementAndGet();
            return orderResponse(1L, 100L);
        });

        assertThat(calls).hasValue(1);
        assertThat(response.data().id()).isEqualTo(1L);
        verify(valueOperations).set(eq("idem:order:create:100:abc"), org.mockito.ArgumentMatchers.contains("COMPLETED"), eq(COMPLETED_TTL));
    }

    @Test
    void completedDuplicateReturnsStoredResponseWithoutCallingAction() throws Exception {
        ApiResponse<OrderResponse> stored = ApiResponse.of(orderResponse(5L, 100L));
        String completed = objectMapper.writeValueAsString(new com.nchuy099.ecommerce.order.idempotency.IdempotencyRecord(
                com.nchuy099.ecommerce.order.idempotency.IdempotencyState.COMPLETED,
                stored
        ));
        when(valueOperations.setIfAbsent(eq("idem:order:create:100:abc"), org.mockito.ArgumentMatchers.anyString(), eq(PROCESSING_TTL)))
                .thenReturn(false);
        when(valueOperations.get("idem:order:create:100:abc")).thenReturn(completed);

        ApiResponse<OrderResponse> response = service.executeCreateOrder("100", "abc", () -> {
            throw new AssertionError("duplicate must not execute action");
        });

        assertThat(response.data().id()).isEqualTo(5L);
    }

    @Test
    void inProgressDuplicateFailsWithoutCallingAction() throws Exception {
        String processing = objectMapper.writeValueAsString(new com.nchuy099.ecommerce.order.idempotency.IdempotencyRecord(
                com.nchuy099.ecommerce.order.idempotency.IdempotencyState.PROCESSING,
                null
        ));
        when(valueOperations.setIfAbsent(eq("idem:order:create:100:abc"), org.mockito.ArgumentMatchers.anyString(), eq(PROCESSING_TTL)))
                .thenReturn(false);
        when(valueOperations.get("idem:order:create:100:abc")).thenReturn(processing);

        assertThatThrownBy(() -> service.executeCreateOrder("100", "abc", () -> orderResponse(1L, 100L)))
                .isInstanceOf(IdempotencyInProgressException.class);
    }

    @Test
    void failedFirstRequestDeletesProcessingKey() {
        when(valueOperations.setIfAbsent(eq("idem:order:create:100:abc"), org.mockito.ArgumentMatchers.anyString(), eq(PROCESSING_TTL)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.executeCreateOrder("100", "abc", () -> {
            throw new RuntimeException("product unavailable");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("product unavailable");

        verify(redisTemplate).delete("idem:order:create:100:abc");
    }

    @Test
    void sameIdempotencyKeyIsScopedByCaller() {
        when(valueOperations.setIfAbsent(eq("idem:order:create:100:abc"), org.mockito.ArgumentMatchers.anyString(), eq(PROCESSING_TTL)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(eq("idem:order:create:200:abc"), org.mockito.ArgumentMatchers.anyString(), eq(PROCESSING_TTL)))
                .thenReturn(true);

        service.executeCreateOrder("100", "abc", () -> orderResponse(1L, 100L));
        service.executeCreateOrder("200", "abc", () -> orderResponse(2L, 200L));

        verify(valueOperations).setIfAbsent(eq("idem:order:create:100:abc"), org.mockito.ArgumentMatchers.anyString(), eq(PROCESSING_TTL));
        verify(valueOperations).setIfAbsent(eq("idem:order:create:200:abc"), org.mockito.ArgumentMatchers.anyString(), eq(PROCESSING_TTL));
    }

    private OrderResponse orderResponse(Long id, Long userId) {
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        return new OrderResponse(
                id,
                "ORD-" + id,
                userId,
                OrderStatus.CONFIRMED,
                new BigDecimal("100.00"),
                List.of(new OrderItemResponse(1L, 10L, "Phone", 1, new BigDecimal("100.00"), new BigDecimal("100.00"))),
                now,
                now
        );
    }
}

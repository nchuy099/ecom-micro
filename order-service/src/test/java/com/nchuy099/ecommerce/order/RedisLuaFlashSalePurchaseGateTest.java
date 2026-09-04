package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import com.nchuy099.ecommerce.order.entity.FlashSaleCampaignEntity;
import com.nchuy099.ecommerce.order.service.FlashSalePurchaseResult;
import com.nchuy099.ecommerce.order.service.RedisLuaFlashSalePurchaseGate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedisLuaFlashSalePurchaseGateTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void acceptedResultInitializesStockAndRunsLuaAtomically() {
        FlashSaleCampaignEntity campaign = campaign();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("1"), eq("2"))).thenReturn(0L);

        RedisLuaFlashSalePurchaseGate gate = new RedisLuaFlashSalePurchaseGate(redisTemplate);

        assertThat(gate.tryPurchase(campaign, 100L, 1)).isEqualTo(FlashSalePurchaseResult.ACCEPTED);
        verify(valueOperations).setIfAbsent("flashsale:campaign:1:stock", "5");
        verify(redisTemplate).execute(any(RedisScript.class), eq(java.util.List.of(
                "flashsale:campaign:1:stock",
                "flashsale:campaign:1:user:100"
        )), eq("1"), eq("2"));
    }

    @Test
    void mapsLuaRejectionCodes() {
        FlashSaleCampaignEntity campaign = campaign();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("1"), eq("2")))
                .thenReturn(1L)
                .thenReturn(2L);

        RedisLuaFlashSalePurchaseGate gate = new RedisLuaFlashSalePurchaseGate(redisTemplate);

        assertThat(gate.tryPurchase(campaign, 100L, 1)).isEqualTo(FlashSalePurchaseResult.SOLD_OUT);
        assertThat(gate.tryPurchase(campaign, 100L, 1)).isEqualTo(FlashSalePurchaseResult.ALREADY_PURCHASED);
    }

    private static FlashSaleCampaignEntity campaign() {
        FlashSaleCampaignEntity campaign = new FlashSaleCampaignEntity(
                10L,
                5,
                Instant.parse("2026-09-05T00:00:00Z"),
                Instant.parse("2026-09-06T00:00:00Z"),
                new BigDecimal("49.99"),
                2
        );
        ReflectionTestUtils.setField(campaign, "id", 1L);
        return campaign;
    }
}

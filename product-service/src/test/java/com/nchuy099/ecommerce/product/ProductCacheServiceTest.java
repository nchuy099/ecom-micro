package com.nchuy099.ecommerce.product;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.service.ProductCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private Cache<Long, ProductResponse> l1Cache;
    private ObjectMapper objectMapper;
    private ProductCacheService cacheService;

    private final ProductResponse sampleProduct = new ProductResponse(
            1L,
            "Keyboard",
            "KB-001",
            new BigDecimal("99.99"),
            10,
            5L,
            0L,
            Instant.parse("2026-09-01T10:00:00Z"),
            Instant.parse("2026-09-01T10:00:00Z")
    );

    @BeforeEach
    void setUp() {
        l1Cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(5))
                .maximumSize(100)
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cacheService = new ProductCacheService(l1Cache, redisTemplate, objectMapper);
    }

    @Test
    void returnsFromL1WhenPresentWithoutCallingRedis() {
        l1Cache.put(1L, sampleProduct);

        Optional<ProductResponse> result = cacheService.get(1L);

        assertThat(result).isPresent().contains(sampleProduct);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void returnsFromL2AndPopulatesL1WhenL1Misses() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String json = objectMapper.writeValueAsString(sampleProduct);
        when(valueOperations.get("product:1")).thenReturn(json);

        Optional<ProductResponse> result = cacheService.get(1L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().name()).isEqualTo("Keyboard");

        // Verify populated in L1
        ProductResponse inL1 = l1Cache.getIfPresent(1L);
        assertThat(inL1).isNotNull();
        assertThat(inL1.sku()).isEqualTo("KB-001");
    }

    @Test
    void returnsEmptyWhenBothL1AndL2Miss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:2")).thenReturn(null);

        Optional<ProductResponse> result = cacheService.get(2L);

        assertThat(result).isEmpty();
    }

    @Test
    void putStoresInBothL1AndL2WithTtl() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.put(1L, sampleProduct);

        assertThat(l1Cache.getIfPresent(1L)).isEqualTo(sampleProduct);
        String expectedJson = objectMapper.writeValueAsString(sampleProduct);
        verify(valueOperations).set(eq("product:1"), eq(expectedJson), eq(Duration.ofSeconds(60)));
    }

    @Test
    void evictClearsBothL1AndL2() {
        l1Cache.put(1L, sampleProduct);

        cacheService.evict(1L);

        assertThat(l1Cache.getIfPresent(1L)).isNull();
        verify(redisTemplate).delete("product:1");
    }

    @Test
    void handlesRedisReadErrorGracefully() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:1")).thenThrow(new RedisConnectionFailureException("Redis down"));

        Optional<ProductResponse> result = cacheService.get(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void handlesRedisWriteErrorGracefully() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("Redis write error"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        cacheService.put(1L, sampleProduct);

        // L1 is still cached
        assertThat(l1Cache.getIfPresent(1L)).isEqualTo(sampleProduct);
    }

    @Test
    void handlesRedisEvictErrorGracefully() {
        l1Cache.put(1L, sampleProduct);
        doThrow(new RedisConnectionFailureException("Redis delete error"))
                .when(redisTemplate).delete("product:1");

        cacheService.evict(1L);

        // L1 is still evicted
        assertThat(l1Cache.getIfPresent(1L)).isNull();
    }
}

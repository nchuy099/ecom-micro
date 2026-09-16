package com.nchuy099.ecommerce.product.service;

import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {
    private static final String KEY_PREFIX = "product:";
    private static final Duration L2_TTL = Duration.ofSeconds(60);

    private final Cache<Long, ProductResponse> l1Cache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<ProductResponse> get(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        ProductResponse l1Cached = l1Cache.getIfPresent(id);
        if (l1Cached != null) {
            log.debug("L1 cache hit for product id {}", id);
            return Optional.of(l1Cached);
        }

        try {
            String redisValue = redisTemplate.opsForValue().get(cacheKey(id));
            if (redisValue != null && !redisValue.isBlank()) {
                ProductResponse response = objectMapper.readValue(redisValue, ProductResponse.class);
                l1Cache.put(id, response);
                log.debug("L2 cache hit for product id {}, populated L1", id);
                return Optional.of(response);
            }
        } catch (Exception ex) {
            log.warn("Redis L2 cache read failure for product id {}: {}", id, ex.getMessage());
        }

        return Optional.empty();
    }

    public void put(Long id, ProductResponse response) {
        if (id == null || response == null) {
            return;
        }

        l1Cache.put(id, response);

        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey(id), json, L2_TTL);
            log.debug("Populated L1 and L2 cache for product id {}", id);
        } catch (Exception ex) {
            log.warn("Redis L2 cache write failure for product id {}: {}", id, ex.getMessage());
        }
    }

    public void evict(Long id) {
        if (id == null) {
            return;
        }

        l1Cache.invalidate(id);

        try {
            redisTemplate.delete(cacheKey(id));
            log.debug("Evicted product id {} from L1 and L2 cache", id);
        } catch (Exception ex) {
            log.warn("Redis L2 cache evict failure for product id {}: {}", id, ex.getMessage());
        }
    }

    private String cacheKey(Long id) {
        return KEY_PREFIX + id;
    }
}

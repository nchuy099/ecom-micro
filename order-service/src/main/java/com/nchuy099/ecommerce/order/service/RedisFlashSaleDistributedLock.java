package com.nchuy099.ecommerce.order.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisFlashSaleDistributedLock implements FlashSaleDistributedLock {
    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration waitTimeout;
    private final Duration leaseTime;

    public RedisFlashSaleDistributedLock(
            StringRedisTemplate redisTemplate,
            @Value("${flash-sale.lock.wait-timeout:2s}") Duration waitTimeout,
            @Value("${flash-sale.lock.lease-time:10s}") Duration leaseTime
    ) {
        this.redisTemplate = redisTemplate;
        this.waitTimeout = waitTimeout;
        this.leaseTime = leaseTime;
    }

    @Override
    public String tryLock(Long campaignId) {
        String token = UUID.randomUUID().toString();
        String key = lockKey(campaignId);
        long deadline = System.nanoTime() + waitTimeout.toNanos();
        do {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, leaseTime);
            if (Boolean.TRUE.equals(acquired)) {
                return token;
            }
            if (System.nanoTime() >= deadline) {
                return null;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return null;
            }
        } while (true);
    }

    @Override
    public void unlock(Long campaignId, String token) {
        if (token == null) {
            return;
        }
        redisTemplate.execute(RELEASE_SCRIPT, java.util.List.of(lockKey(campaignId)), token);
    }

    static String lockKey(Long campaignId) {
        return "flashsale:lock:campaign:" + campaignId;
    }
}

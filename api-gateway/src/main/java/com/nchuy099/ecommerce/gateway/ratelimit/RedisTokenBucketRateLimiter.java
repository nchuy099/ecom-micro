package com.nchuy099.ecommerce.gateway.ratelimit;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class RedisTokenBucketRateLimiter implements TokenBucketRateLimiter {
    private static final RedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
            local tokens_key = KEYS[1]
            local timestamp_key = KEYS[2]
            local capacity = tonumber(ARGV[1])
            local refill_period = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = 1

            local current_tokens = tonumber(redis.call("get", tokens_key))
            if current_tokens == nil then
                current_tokens = capacity
            end

            local last_refreshed = tonumber(redis.call("get", timestamp_key))
            if last_refreshed == nil then
                last_refreshed = now
            end

            local elapsed = math.max(0, now - last_refreshed)
            local refill_rate = capacity / refill_period
            local filled_tokens = math.min(capacity, current_tokens + (elapsed * refill_rate))

            local allowed = filled_tokens >= requested
            local new_tokens = filled_tokens
            local retry_after = 0
            if allowed then
                new_tokens = filled_tokens - requested
            else
                retry_after = math.ceil((requested - filled_tokens) / refill_rate)
            end

            local ttl = math.max(1, math.ceil(refill_period * 2))
            redis.call("setex", tokens_key, ttl, tostring(new_tokens))
            redis.call("setex", timestamp_key, ttl, tostring(now))

            return { allowed and 1 or 0, math.floor(new_tokens), retry_after }
            """, List.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Clock clock;

    @Autowired
    public RedisTokenBucketRateLimiter(ReactiveStringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    RedisTokenBucketRateLimiter(ReactiveStringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    @Override
    public Mono<RateLimitResult> consume(String key, int capacity, long refillPeriodSeconds) {
        long now = clock.millis() / 1000;
        List<String> keys = List.of(key + ":tokens", key + ":timestamp");
        return redisTemplate.execute(TOKEN_BUCKET_SCRIPT, keys,
                        String.valueOf(capacity),
                        String.valueOf(Math.max(1, refillPeriodSeconds)),
                        String.valueOf(now))
                .next()
                .map(this::toResult);
    }

    private RateLimitResult toResult(List values) {
        boolean allowed = longValue(values.get(0)) == 1;
        long remaining = longValue(values.get(1));
        long retryAfter = longValue(values.get(2));
        return allowed ? RateLimitResult.allowed(remaining) : RateLimitResult.rejected(retryAfter);
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}

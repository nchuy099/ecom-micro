package com.nchuy099.ecommerce.gateway.ratelimit;

import reactor.core.publisher.Mono;

public interface TokenBucketRateLimiter {
    Mono<RateLimitResult> consume(String key, int capacity, long refillPeriodSeconds);
}

package com.nchuy099.ecommerce.gateway.ratelimit;

public record RateLimitResult(
        boolean allowed,
        long remaining,
        long retryAfterSeconds
) {
    public static RateLimitResult allowed(long remaining) {
        return new RateLimitResult(true, remaining, 0);
    }

    public static RateLimitResult rejected(long retryAfterSeconds) {
        return new RateLimitResult(false, 0, Math.max(1, retryAfterSeconds));
    }
}

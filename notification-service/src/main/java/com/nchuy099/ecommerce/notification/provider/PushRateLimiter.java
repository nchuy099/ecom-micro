package com.nchuy099.ecommerce.notification.provider;

import java.util.concurrent.locks.LockSupport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PushRateLimiter {
    private final long intervalNanos;
    private long nextPermitNanos;

    public PushRateLimiter(@Value("${notification.push.rate-per-second:100}") int ratePerSecond) {
        this.intervalNanos = ratePerSecond <= 0 ? 0 : 1_000_000_000L / ratePerSecond;
    }

    public synchronized void acquire() {
        if (intervalNanos == 0) {
            return;
        }
        long now = System.nanoTime();
        long permit = Math.max(now, nextPermitNanos);
        long wait = permit - now;
        if (wait > 0) {
            LockSupport.parkNanos(wait);
        }
        nextPermitNanos = permit + intervalNanos;
    }
}

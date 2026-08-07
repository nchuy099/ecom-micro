package com.nchuy099.ecommerce.order.exception;

public class IdempotencyInProgressException extends RuntimeException {
    private final long retryAfterSeconds;

    public IdempotencyInProgressException(long retryAfterSeconds) {
        super("A request with the same Idempotency-Key is still processing");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

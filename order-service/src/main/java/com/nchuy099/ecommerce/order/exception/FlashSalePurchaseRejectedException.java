package com.nchuy099.ecommerce.order.exception;

public class FlashSalePurchaseRejectedException extends RuntimeException {
    private final Reason reason;

    public FlashSalePurchaseRejectedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        NOT_ACTIVE,
        SOLD_OUT,
        ALREADY_PURCHASED
    }
}

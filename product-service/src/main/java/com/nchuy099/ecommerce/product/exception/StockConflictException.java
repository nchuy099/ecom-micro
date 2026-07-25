package com.nchuy099.ecommerce.product.exception;

public class StockConflictException extends RuntimeException {
    public StockConflictException(Long id) {
        super("Concurrent stock modification for product: " + id);
    }
}

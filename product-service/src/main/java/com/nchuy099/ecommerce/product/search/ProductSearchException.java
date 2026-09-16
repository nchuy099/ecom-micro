package com.nchuy099.ecommerce.product.search;

public class ProductSearchException extends RuntimeException {
    public ProductSearchException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductSearchException(String message) {
        super(message);
    }
}

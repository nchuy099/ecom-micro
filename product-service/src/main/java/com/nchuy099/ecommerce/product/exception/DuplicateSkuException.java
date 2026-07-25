package com.nchuy099.ecommerce.product.exception;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException() {
        super("Product SKU already exists");
    }
}

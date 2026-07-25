package com.nchuy099.ecommerce.product.exception;

public class InvalidPriceRangeException extends RuntimeException {
    public InvalidPriceRangeException() {
        super("minPrice must be less than or equal to maxPrice");
    }
}

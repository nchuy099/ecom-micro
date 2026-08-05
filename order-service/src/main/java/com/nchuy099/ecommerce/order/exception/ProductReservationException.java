package com.nchuy099.ecommerce.order.exception;

import org.springframework.http.HttpStatusCode;

public class ProductReservationException extends RuntimeException {
    private final HttpStatusCode statusCode;

    public ProductReservationException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ProductReservationException(String message, Throwable cause, HttpStatusCode statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}

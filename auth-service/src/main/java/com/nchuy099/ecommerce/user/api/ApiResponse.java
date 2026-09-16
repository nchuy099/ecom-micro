package com.nchuy099.ecommerce.user.api;

public record ApiResponse<T>(T data, Meta meta) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, new Meta(null));
    }

    public record Meta(String traceId) {
    }
}

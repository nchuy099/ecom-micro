package com.nchuy099.ecommerce.product.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        Integer page,
        int size,
        Long totalElements,
        Integer totalPages,
        String nextCursor,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages, null, false);
    }

    public static <T> PageResponse<T> cursor(List<T> content, int size, String nextCursor, boolean hasNext) {
        return new PageResponse<>(content, null, size, null, null, nextCursor, hasNext);
    }
}

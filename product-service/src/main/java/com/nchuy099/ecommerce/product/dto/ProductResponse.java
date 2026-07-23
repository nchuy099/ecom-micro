package com.nchuy099.ecommerce.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.nchuy099.ecommerce.product.entity.ProductEntity;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        Integer stock,
        Long categoryId,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getStock(),
                product.getCategoryId(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

package com.nchuy099.ecommerce.product.search;

import java.math.BigDecimal;
import java.time.Instant;

import com.nchuy099.ecommerce.product.dto.ProductResponse;
import com.nchuy099.ecommerce.product.entity.ProductEntity;

public record ProductDocument(
        Long id,
        String name,
        String sku,
        Long categoryId,
        BigDecimal price,
        Integer stock,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductDocument from(ProductEntity product) {
        return new ProductDocument(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCategoryId(),
                product.getPrice(),
                product.getStock(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductResponse toResponse() {
        return new ProductResponse(id, name, sku, price, stock, categoryId, version, createdAt, updatedAt);
    }
}

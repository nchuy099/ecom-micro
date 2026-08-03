package com.nchuy099.ecommerce.order.client.dto;

import java.math.BigDecimal;

public record ProductClientResponse(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        Integer stock,
        Long categoryId,
        Long version
) {
}

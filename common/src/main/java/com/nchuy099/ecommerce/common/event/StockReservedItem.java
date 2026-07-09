package com.nchuy099.ecommerce.common.event;

import java.math.BigDecimal;

public record StockReservedItem(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}

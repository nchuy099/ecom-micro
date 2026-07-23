package com.nchuy099.ecommerce.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockQuantityRequest(@NotNull @Min(1) Integer quantity) {
}

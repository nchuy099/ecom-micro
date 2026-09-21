package com.nchuy099.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateFlashSaleCampaignRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive Integer stock,
        @NotNull @Future Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull @DecimalMin("0.0") BigDecimal promoPrice,
        @NotNull @Positive Integer maxPerUser,
        @NotEmpty List<@NotNull @Positive Long> recipientUserIds,
        String subject,
        String message
) {
}

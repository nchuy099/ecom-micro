package com.nchuy099.ecommerce.order.idempotency;

import com.nchuy099.ecommerce.common.ApiResponse;
import com.nchuy099.ecommerce.order.dto.OrderResponse;

public record IdempotencyRecord(
        IdempotencyState state,
        ApiResponse<OrderResponse> response
) {
    static IdempotencyRecord processing() {
        return new IdempotencyRecord(IdempotencyState.PROCESSING, null);
    }

    static IdempotencyRecord completed(ApiResponse<OrderResponse> response) {
        return new IdempotencyRecord(IdempotencyState.COMPLETED, response);
    }
}

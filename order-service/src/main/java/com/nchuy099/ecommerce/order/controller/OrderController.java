package com.nchuy099.ecommerce.order.controller;

import com.nchuy099.ecommerce.order.api.ApiResponse;
import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.dto.PageResponse;
import com.nchuy099.ecommerce.order.idempotency.OrderIdempotencyService;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final OrderService orderService;
    private final OrderIdempotencyService orderIdempotencyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> create(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(name = USER_ID_HEADER, required = false) String gatewayUserId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ApiResponse.of(orderService.create(request));
        }
        String callerId = gatewayUserId != null && !gatewayUserId.isBlank()
                ? gatewayUserId
                : String.valueOf(request.userId());
        return orderIdempotencyService.executeCreateOrder(callerId, idempotencyKey, () -> orderService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> findById(@PathVariable Long id) {
        return ApiResponse.of(orderService.findById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> listOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (cursor != null && page != null) {
            throw BusinessException.badRequest(
                    "https://errors.ecom.local/invalid-pagination",
                    "Invalid pagination request",
                    "page and cursor cannot be used together"
            );
        }
        if (cursor != null) {
            return ApiResponse.of(orderService.findByUserIdCursor(userId, cursor, size));
        }
        return ApiResponse.of(orderService.findByUserId(
                userId,
                PageRequest.of(
                        page == null ? 0 : page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                )
        ));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<PageResponse<OrderResponse>> findByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return listOrders(userId, page, cursor, size);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.of(orderService.cancel(id));
    }
}

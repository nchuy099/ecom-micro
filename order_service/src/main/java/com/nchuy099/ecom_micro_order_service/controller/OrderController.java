package com.nchuy099.ecom_micro_order_service.controller;

import com.nchuy099.ecom_micro_order_service.dto.BaseResponse;
import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;
import com.nchuy099.ecom_micro_order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<BaseResponse<OrderEntity>> create(
            @RequestBody @Valid CreateOrderReq req) {
        log.info(
                "order.request.received customerId={} itemCount={}",
                req.getCustomerId(),
                req.getItems().size());
        OrderEntity order = orderService.create(req);
        log.info("order.request.completed orderId={} status={}", order.getId(), order.getStatus());
        return ResponseEntity.ok(new BaseResponse<>(order, "success"));
    }
}

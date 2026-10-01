package com.nchuy099.ecom_micro_order_service.controller;

import com.nchuy099.ecom_micro_order_service.dto.BaseResponse;
import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;
import com.nchuy099.ecom_micro_order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<BaseResponse<OrderEntity>> create(@RequestBody @Valid CreateOrderReq req) {


        return ResponseEntity.ok(new BaseResponse<>(orderService.create(req), "success"));
    }
}

package com.nchuy099.ecom_micro_order_service.service.impl;

import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;
import com.nchuy099.ecom_micro_order_service.entity.OrderItemEntity;
import com.nchuy099.ecom_micro_order_service.repository.OrderRepository;
import com.nchuy099.ecom_micro_order_service.repository.OrderItemRepository;
import com.nchuy099.ecom_micro_order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public OrderEntity create(CreateOrderReq req) {
        var order = OrderEntity.builder()
                .customerId(req.getCustomerId())
                .status(req.getStatus())
                .totalAmount(req.getTotalAmount())
                .build();
        var savedOrder = orderRepository.save(order);
        var items = req.getItems().stream()
                .map(item -> OrderItemEntity.builder()
                        .orderId(savedOrder.getId())
                        .productId(item.getProductId())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
        orderItemRepository.saveAll(items);
        return savedOrder;
    }
}

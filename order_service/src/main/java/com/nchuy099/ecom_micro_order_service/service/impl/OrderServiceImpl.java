package com.nchuy099.ecom_micro_order_service.service.impl;

import com.nchuy099.ecom_micro_order_service.clients.ProductClient;
import com.nchuy099.ecom_micro_order_service.dto.ProductResp;
import com.nchuy099.ecom_micro_order_service.dto.event.OrderCreatedEvent;
import com.nchuy099.ecom_micro_order_service.dto.event.OrderCreatedItem;
import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.dto.request.ProductFilter;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;
import com.nchuy099.ecom_micro_order_service.entity.OrderItemEntity;
import com.nchuy099.ecom_micro_order_service.exception.ApplicationException;
import com.nchuy099.ecom_micro_order_service.repository.OrderRepository;
import com.nchuy099.ecom_micro_order_service.repository.OrderItemRepository;
import com.nchuy099.ecom_micro_order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    // bean ma kafka tao cho

    @Override
    @Transactional
    public OrderEntity create(CreateOrderReq req) {
        var productFilter = new ProductFilter();
        productFilter.setIds(req.getItems().stream()
                .map(item -> item.getProductId())
                .distinct()
                .toList());

        Map<String, ProductResp> products = productClient.getProductsByIds(productFilter)
                .stream()
                .collect(Collectors.toMap(ProductResp::getId, Function.identity()));

        for (var item : req.getItems()) {
            var product = products.get(item.getProductId());
            if (product == null) {
                throw new ApplicationException("Product " + item.getProductId() + " not found");
            }
            if (item.getQuantity() > product.getStock()) {
                throw new ApplicationException("Product " + item.getProductId() + " not enough");
            }
        }

        int totalAmount = 0;
        List<OrderItemEntity> items = new ArrayList<>();
        var order = OrderEntity.builder()
                .customerId(req.getCustomerId())
                .status("NEW")
                .totalAmount(0)
                .build();
        var savedOrder = orderRepository.save(order);

        for (var item : req.getItems()) {
            var product = products.get(item.getProductId());
            items.add(OrderItemEntity.builder()
                    .orderId(savedOrder.getId())
                    .productId(item.getProductId())
                    .price(product.getPrice())
                    .quantity(item.getQuantity())
                    .build());
            totalAmount += product.getPrice() * item.getQuantity();
        }

        savedOrder.setTotalAmount(totalAmount);
        OrderEntity createdOrder = orderRepository.save(savedOrder);
        orderItemRepository.saveAll(items);

        var orderCreatedEvent = OrderCreatedEvent.builder()
                .id(createdOrder.getId())
                .customerId(createdOrder.getCustomerId())
                .status(createdOrder.getStatus())
                .totalAmount(createdOrder.getTotalAmount())
                .orderItems(items.stream()
                        .map(item -> OrderCreatedItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();

        kafkaTemplate.send("order-created", orderCreatedEvent);
        log.info("Publish new order success to order_created");
        return savedOrder;
    }

    @Override
    public void updateOrderStatusLocked(String orderId) {
        Optional<OrderEntity> optOrder = orderRepository.findById(orderId);
        if (optOrder.isEmpty()) {
            throw new ApplicationException("Order not found");
        }

        OrderEntity order = optOrder.get();

        order.setStatus("LOCKED");
        orderRepository.save(order);

    }
}

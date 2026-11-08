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
import com.nchuy099.ecom_micro_order_service.saga.SagaOutbox;
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
    private final SagaOutbox outbox;
    // bean ma kafka tao cho

    @Override
    @Transactional
    public OrderEntity create(CreateOrderReq req) {
        log.info("order.create.start customerId={} itemCount={}",
                req.getCustomerId(), req.getItems().size());
        var productFilter = new ProductFilter();
        productFilter.setIds(req.getItems().stream()
                .map(item -> item.getProductId())
                .distinct()
                .toList());

        Map<String, ProductResp> products = productClient.getProductsByIds(productFilter)
                .stream()
                .collect(Collectors.toMap(ProductResp::getId, Function.identity()));
        log.info("order.products.loaded customerId={} requestedProducts={} foundProducts={}",
                req.getCustomerId(), productFilter.getIds().size(), products.size());

        for (var item : req.getItems()) {
            var product = products.get(item.getProductId());
            if (product == null) {
                log.warn("order.validation.failed reason=product_not_found productId={}", item.getProductId());
                throw new ApplicationException("Product " + item.getProductId() + " not found");
            }
            if (item.getQuantity() > product.getStock()) {
                log.warn("order.validation.failed reason=insufficient_stock productId={} requested={} available={}",
                        item.getProductId(), item.getQuantity(), product.getStock());
                throw new ApplicationException("Product " + item.getProductId() + " not enough");
            }
        }
        log.info("order.validation.completed customerId={}", req.getCustomerId());

        int totalAmount = 0;
        List<OrderItemEntity> items = new ArrayList<>();
        var order = OrderEntity.builder()
                .customerId(req.getCustomerId())
                .status("PENDING_STOCK")
                .totalAmount(0)
                .build();
        var savedOrder = orderRepository.save(order);
        log.info("order.persisted orderId={} status={}", savedOrder.getId(), savedOrder.getStatus());

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
        log.info("order.items.persisted orderId={} itemCount={} totalAmount={}",
                createdOrder.getId(), items.size(), createdOrder.getTotalAmount());

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

        outbox.add("order-created", createdOrder.getId(), orderCreatedEvent);
        log.info("order.create.completed orderId={} status={}", createdOrder.getId(), createdOrder.getStatus());
        return savedOrder;
    }

    @Override @Transactional
    public void updateOrderStatusLocked(String orderId) {
        orderRepository.findForUpdate(orderId).ifPresent(order -> {
            if ("PENDING_STOCK".equals(order.getStatus())) order.setStatus("PENDING_PAYMENT");
        });
    }

    @Override @Transactional
    public void confirm(String orderId) {
        orderRepository.findForUpdate(orderId).ifPresent(order -> {
            if ("PENDING_PAYMENT".equals(order.getStatus()) || "PENDING_STOCK".equals(order.getStatus())) {
                order.setStatus("CONFIRMED");
                outbox.add("order-confirmed", orderId, Map.of("orderId", orderId));
            }
        });
    }

    @Override @Transactional
    public void cancel(String orderId, String reason) {
        orderRepository.findForUpdate(orderId).ifPresent(order -> {
            if (!"CANCELLED".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                var items = orderItemRepository.findByOrderId(orderId).stream()
                        .map(item -> Map.of("productId", item.getProductId(), "quantity", item.getQuantity())).toList();
                outbox.add("order-cancelled", orderId, Map.of("orderId", orderId, "reason", reason, "orderItems", items));
            }
        });
    }
}

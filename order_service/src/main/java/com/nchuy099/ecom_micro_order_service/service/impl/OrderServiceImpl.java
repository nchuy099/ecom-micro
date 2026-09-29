package com.nchuy099.ecom_micro_order_service.service.impl;

import com.nchuy099.ecom_micro_order_service.clients.ProductClient;
import com.nchuy099.ecom_micro_order_service.dto.ProductResp;
import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.dto.request.ProductFilter;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;
import com.nchuy099.ecom_micro_order_service.entity.OrderItemEntity;
import com.nchuy099.ecom_micro_order_service.exception.ApplicationException;
import com.nchuy099.ecom_micro_order_service.repository.OrderRepository;
import com.nchuy099.ecom_micro_order_service.repository.OrderItemRepository;
import com.nchuy099.ecom_micro_order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;

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
        orderRepository.save(savedOrder);
        orderItemRepository.saveAll(items);
        return savedOrder;
    }
}

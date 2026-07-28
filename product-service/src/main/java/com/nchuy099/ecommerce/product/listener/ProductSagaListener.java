package com.nchuy099.ecommerce.product.listener;

import com.nchuy099.ecommerce.common.event.KafkaTopics;
import com.nchuy099.ecommerce.common.event.OrderCancelledEvent;
import com.nchuy099.ecommerce.common.event.OrderCreatedEvent;
import com.nchuy099.ecommerce.product.service.ProductService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductSagaListener {
    private final ProductService productService;

    public ProductSagaListener(ProductService productService) {
        this.productService = productService;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "product-service-saga")
    public void handleOrderCreated(OrderCreatedEvent event) {
        productService.reserveForOrder(event);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "product-service-saga")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        productService.releaseForOrder(event);
    }
}

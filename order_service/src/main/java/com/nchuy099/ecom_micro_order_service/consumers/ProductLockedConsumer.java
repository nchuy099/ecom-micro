package com.nchuy099.ecom_micro_order_service.consumers;

import com.nchuy099.ecom_micro_order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLockedConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "product-locked")
    public void handleProductLockedEvent(String orderId) {
        log.info("Receive product locked message for order {}", orderId);
        orderService.updateOrderStatusLocked(orderId);
        log.info("Success update order status LOCKED");
    }
}

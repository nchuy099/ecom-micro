package com.nchuy099.ecommerce.order.listener;

import com.nchuy099.ecommerce.common.event.KafkaTopics;
import com.nchuy099.ecommerce.common.event.PaymentCompletedEvent;
import com.nchuy099.ecommerce.common.event.PaymentFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservationFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedEvent;
import com.nchuy099.ecommerce.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaListener {
    private final OrderService orderService;

    public OrderSagaListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.STOCK_RESERVED, groupId = "order-service-saga")
    public void handleStockReserved(StockReservedEvent event) {
        orderService.applyStockReserved(event);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "order-service-saga")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        orderService.applyPaymentCompleted(event);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "order-service-saga")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        orderService.applyPaymentFailed(event);
    }

    @KafkaListener(topics = KafkaTopics.STOCK_RESERVATION_FAILED, groupId = "order-service-saga")
    public void handleStockReservationFailed(StockReservationFailedEvent event) {
        orderService.applyStockReservationFailed(event);
    }
}

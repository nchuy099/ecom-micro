package com.nchuy099.ecom_micro_product_service.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecom_micro_product_service.consumers.dto.OrderCreatedEvent;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductItem;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductReq;
import com.nchuy099.ecom_micro_product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final ProductService productService;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @KafkaListener(topics = "order-created")
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            exclude = {NullPointerException.class, IllegalArgumentException.class}
    )
    public void handleOrderCreatedEvent(String orderString) throws JsonProcessingException {
        log.info("Receive order message {}", orderString);
        ObjectMapper objectMapper = new ObjectMapper();
        OrderCreatedEvent orderCreatedEvent = objectMapper.readValue(
                orderString,
                OrderCreatedEvent.class);
//        throw new RuntimeException("hihi");

        if (orderCreatedEvent.getOrderItems() == null
                || orderCreatedEvent.getOrderItems().isEmpty()) {
            log.warn("Skip order {} because it has no order items", orderCreatedEvent.getId());
            return;
        }

        var lockProductItems = new ArrayList<LockProductItem>();
        orderCreatedEvent.getOrderItems().forEach(orderItem -> {
            var lockProductItem = new LockProductItem();
            lockProductItem.setId(orderItem.getProductId());
            lockProductItem.setQuantity(orderItem.getQuantity());
            lockProductItems.add(lockProductItem);
        });

        var lockProductReq = new LockProductReq();
        lockProductReq.setItems(lockProductItems);

        productService.lock(lockProductReq);
        log.info("Success to lock product item of order {}", orderCreatedEvent.getId());
        kafkaTemplate.send("product-locked", orderCreatedEvent.getId());
        log.info("Success publishing product-locked event of order {}", orderCreatedEvent.getId());

    }
}

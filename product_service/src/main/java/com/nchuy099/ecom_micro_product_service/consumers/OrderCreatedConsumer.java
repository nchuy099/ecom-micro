package com.nchuy099.ecom_micro_product_service.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecom_micro_product_service.saga.StockSaga;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {
    private final StockSaga stock;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "order-created")
    public void reserve(String json) throws Exception {
        stock.reserve(mapper.readTree(json));
    }

    @KafkaListener(topics = "order-cancelled")
    public void cancel(String json) throws Exception {
        stock.cancel(mapper.readTree(json));
    }
}

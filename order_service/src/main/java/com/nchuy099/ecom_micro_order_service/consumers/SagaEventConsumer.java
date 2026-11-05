package com.nchuy099.ecom_micro_order_service.consumers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecom_micro_order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SagaEventConsumer {
    private final OrderService orders;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = "stock-reserved")
    public void stockReserved(String json) throws Exception {
        orders.updateOrderStatusLocked(mapper.readTree(json).path("orderId").asText());
    }

    @KafkaListener(topics = "payment-completed")
    public void paymentCompleted(String json) throws Exception {
        orders.confirm(mapper.readTree(json).path("orderId").asText());
    }

    @KafkaListener(topics = {"stock-released", "stock-reservation-failed"})
    public void cancelled(String json) throws Exception {
        orders.cancel(mapper.readTree(json).path("orderId").asText());
    }
}

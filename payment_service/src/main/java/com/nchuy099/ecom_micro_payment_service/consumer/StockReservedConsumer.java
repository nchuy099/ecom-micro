package com.nchuy099.ecom_micro_payment_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StockReservedConsumer {
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${saga.payment.fail:false}")
    private boolean fail;

    @KafkaListener(topics = "stock-reserved")
    public void handle(String json) throws Exception {
        JsonNode e = mapper.readTree(json);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", e.path("orderId").asText());
        result.put("orderItems", e.path("orderItems"));
        kafka.send(
                fail ? "payment-failed" : "payment-completed", e.path("orderId").asText(), result);
    }
}

package com.nchuy099.ecom_micro_product_service.consumers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductItem;
import com.nchuy099.ecom_micro_product_service.dto.request.LockProductReq;
import com.nchuy099.ecom_micro_product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {
    private final ProductService productService;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = "order-created")
    public void reserve(String json) throws Exception {
        JsonNode event = mapper.readTree(json);
        String id = event.path("id").asText();
        try {
            var items = new ArrayList<LockProductItem>();
            event.path("orderItems")
                    .forEach(
                            n -> {
                                LockProductItem i = new LockProductItem();
                                i.setId(n.path("productId").asText());
                                i.setQuantity(n.path("quantity").asInt());
                                items.add(i);
                            });
            LockProductReq req = new LockProductReq();
            req.setItems(items);
            productService.lock(req);
            publish("stock-reserved", payload(id, event.path("orderItems")));
        } catch (RuntimeException ex) {
            publish(
                    "stock-reservation-failed",
                    Map.of("orderId", id, "reason", String.valueOf(ex.getMessage())));
        }
    }

    @KafkaListener(topics = "payment-failed")
    public void release(String json) throws Exception {
        JsonNode event = mapper.readTree(json);
        var items = new ArrayList<LockProductItem>();
        event.path("orderItems")
                .forEach(
                        n -> {
                            LockProductItem i = new LockProductItem();
                            i.setId(n.path("productId").asText());
                            i.setQuantity(-n.path("quantity").asInt());
                            items.add(i);
                        });
        LockProductReq req = new LockProductReq();
        req.setItems(items);
        productService.lock(req);
        publish(
                "stock-released",
                payload(event.path("orderId").asText(), event.path("orderItems")));
    }

    private Map<String, Object> payload(String id, JsonNode items) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", id);
        m.put("orderItems", items);
        return m;
    }

    private void publish(String topic, Object value) {
        String key = value instanceof Map<?, ?> m ? String.valueOf(m.get("orderId")) : "";
        kafka.send(topic, key, value);
    }
}

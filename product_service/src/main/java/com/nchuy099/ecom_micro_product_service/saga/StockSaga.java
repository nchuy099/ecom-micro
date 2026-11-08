package com.nchuy099.ecom_micro_product_service.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecom_micro_product_service.repository.ProductRepository;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;

@Service
public class StockSaga {
    private final JdbcTemplate jdbc;
    private final ProductRepository products;
    private final SagaOutbox outbox;
    private final ObjectMapper mapper;
    private final CacheManager cache;

    public StockSaga(
            JdbcTemplate jdbc,
            ProductRepository products,
            SagaOutbox outbox,
            ObjectMapper mapper,
            CacheManager cache) {
        this.jdbc = jdbc;
        this.products = products;
        this.outbox = outbox;
        this.mapper = mapper;
        this.cache = cache;
    }

    private String lock(String id) {
        jdbc.update("INSERT IGNORE INTO saga_state (order_id, state) VALUES (?, 'NEW')", id);
        return jdbc.queryForObject(
                "SELECT state FROM saga_state WHERE order_id = ? FOR UPDATE", String.class, id);
    }

    @Transactional
    public void reserve(JsonNode event) {
        String id = event.required("id").asText();
        if (!"NEW".equals(lock(id))) return;
        JsonNode items = event.required("orderItems");
        Map<String, Integer> quantities = quantities(items);
        var rows = products.findByIdInForUpdate(new ArrayList<>(quantities.keySet()));
        if (rows.size() != quantities.size()
                || rows.stream().anyMatch(p -> p.getStock() < quantities.get(p.getId()))) {
            jdbc.update("UPDATE saga_state SET state='FAILED' WHERE order_id=?", id);
            outbox.add(
                    "stock-reservation-failed",
                    id,
                    Map.of("orderId", id, "reason", "Insufficient stock or product not found"));
            return;
        }
        rows.forEach(p -> p.setStock(p.getStock() - quantities.get(p.getId())));
        products.saveAll(rows);
        jdbc.update(
                "UPDATE saga_state SET state='RESERVED', items=? WHERE order_id=?",
                items.toString(),
                id);
        outbox.add("stock-reserved", id, Map.of("orderId", id, "orderItems", items));
        evict(quantities.keySet());
    }

    @Transactional
    public void cancel(JsonNode event) throws Exception {
        String id = event.required("orderId").asText();
        String state = lock(id);
        if ("CANCELLED".equals(state) || "RELEASED".equals(state)) return;
        if ("RESERVED".equals(state)) {
            String json =
                    jdbc.queryForObject(
                            "SELECT items FROM saga_state WHERE order_id=?", String.class, id);
            JsonNode items = mapper.readTree(json);
            Map<String, Integer> quantities = quantities(items);
            var rows = products.findByIdInForUpdate(new ArrayList<>(quantities.keySet()));
            if (rows.size() != quantities.size())
                throw new IllegalStateException("Reserved product no longer exists");
            rows.forEach(p -> p.setStock(p.getStock() + quantities.get(p.getId())));
            products.saveAll(rows);
            jdbc.update("UPDATE saga_state SET state='RELEASED' WHERE order_id=?", id);
            outbox.add("stock-released", id, Map.of("orderId", id, "orderItems", items));
            evict(quantities.keySet());
        } else jdbc.update("UPDATE saga_state SET state='CANCELLED' WHERE order_id=?", id);
    }

    private Map<String, Integer> quantities(JsonNode items) {
        if (!items.isArray() || items.isEmpty())
            throw new IllegalArgumentException("Order items required");
        Map<String, Integer> result = new TreeMap<>();
        items.forEach(
                item -> {
                    String productId = item.required("productId").asText();
                    int quantity = item.required("quantity").asInt();
                    if (productId.isBlank() || quantity <= 0)
                        throw new IllegalArgumentException("Invalid order item");
                    result.merge(productId, quantity, Math::addExact);
                });
        return result;
    }

    private void evict(Set<String> ids) {
        var c = cache.getCache("products");
        if (c != null)
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            ids.forEach(c::evict);
                        }
                    });
    }
}

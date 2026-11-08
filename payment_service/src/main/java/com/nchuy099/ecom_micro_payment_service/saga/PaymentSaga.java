package com.nchuy099.ecom_micro_payment_service.saga;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class PaymentSaga {
    private final JdbcTemplate jdbc;
    private final SagaOutbox outbox;

    @Value("${saga.payment.fail:false}")
    private boolean fail;

    public PaymentSaga(JdbcTemplate jdbc, SagaOutbox outbox) {
        this.jdbc = jdbc;
        this.outbox = outbox;
    }

    private String lock(String id) {
        jdbc.update("INSERT IGNORE INTO saga_state (order_id, state) VALUES (?, 'NEW')", id);
        return jdbc.queryForObject(
                "SELECT state FROM saga_state WHERE order_id=? FOR UPDATE", String.class, id);
    }

    @Transactional
    public void pay(JsonNode event) {
        String id = event.required("orderId").asText();
        if (!"NEW".equals(lock(id))) return;
        jdbc.update(
                "UPDATE saga_state SET state=? WHERE order_id=?",
                fail ? "FAILED" : "COMPLETED",
                id);
        outbox.add(
                fail ? "payment-failed" : "payment-completed",
                id,
                Map.of(
                        "orderId",
                        id,
                        "reason",
                        fail ? "Payment declined" : "",
                        "orderItems",
                        event.required("orderItems")));
    }

    @Transactional
    public void cancel(JsonNode event) {
        String id = event.required("orderId").asText();
        String state = lock(id);
        if ("COMPLETED".equals(state))
            jdbc.update("UPDATE saga_state SET state='REFUNDED' WHERE order_id=?", id);
        else if ("NEW".equals(state) || "FAILED".equals(state))
            jdbc.update("UPDATE saga_state SET state='CANCELLED' WHERE order_id=?", id);
    }
}

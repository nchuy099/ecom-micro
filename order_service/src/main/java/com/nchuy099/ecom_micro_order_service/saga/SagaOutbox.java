package com.nchuy099.ecom_micro_order_service.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SagaOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public SagaOutbox(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void add(String topic, String orderId, Object payload) {
        try {
            jdbc.update(
                    "INSERT INTO saga_outbox (topic, order_id, payload) VALUES (?, ?, ?)",
                    topic,
                    orderId,
                    mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize saga event", e);
        }
    }
}

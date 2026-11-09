package com.nchuy099.ecom_micro_product_service.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Statement;

@Component
public class SagaOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    public SagaOutbox(JdbcTemplate jdbc, ObjectMapper mapper, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void add(String topic, String orderId, Object payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            var key = new GeneratedKeyHolder();
            jdbc.update(
                    connection -> {
                        var statement =
                                connection.prepareStatement(
                                        "INSERT INTO saga_outbox (topic, order_id, payload) VALUES"
                                            + " (?, ?, ?)",
                                        Statement.RETURN_GENERATED_KEYS);
                        statement.setString(1, topic);
                        statement.setString(2, orderId);
                        statement.setString(3, json);
                        return statement;
                    },
                    key);
            Number id = key.getKey();
            if (id == null) throw new IllegalStateException("Missing generated outbox ID");
            events.publishEvent(new OutboxReady(id.longValue()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize saga event", e);
        }
    }
}

package com.nchuy099.ecom_micro_order_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;

    public OutboxPublisher(
            JdbcTemplate jdbc,
            KafkaTemplate<String, Object> kafka,
            ObjectMapper mapper,
            TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.mapper = mapper;
        this.transactions = transactions;
    }

    @Scheduled(fixedDelayString = "${saga.outbox.poll-ms:1000}")
    public void publish() {
        try {
            transactions.executeWithoutResult(
                    status -> {
                        var rows =
                                jdbc.queryForList(
                                        "SELECT id, topic, order_id, payload FROM saga_outbox ORDER"
                                            + " BY id LIMIT 1 FOR UPDATE");
                        if (rows.isEmpty()) return;
                        var row = rows.getFirst();
                        try {
                            kafka.send(
                                            (String) row.get("topic"),
                                            (String) row.get("order_id"),
                                            mapper.readTree((String) row.get("payload")))
                                    .get(30, TimeUnit.SECONDS);
                            jdbc.update("DELETE FROM saga_outbox WHERE id = ?", row.get("id"));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Outbox publisher interrupted", e);
                        } catch (Exception e) {
                            throw new IllegalStateException("Cannot publish saga outbox event", e);
                        }
                    });
        } catch (Exception e) {
            log.warn("Saga outbox delivery failed; will retry", e);
        }
    }
}

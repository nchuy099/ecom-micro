package com.nchuy099.ecom_micro_payment_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;
    private final TaskExecutor executor;
    private final long sendTimeoutMs;
    private final int batchSize;

    public OutboxPublisher(
            JdbcTemplate jdbc,
            KafkaTemplate<String, Object> kafka,
            ObjectMapper mapper,
            TransactionTemplate transactions,
            @Qualifier("outboxExecutor") TaskExecutor executor,
            @Value("${saga.outbox.send-timeout-ms:30000}") long sendTimeoutMs,
            @Value("${saga.outbox.batch-size:100}") int batchSize) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.mapper = mapper;
        this.transactions = transactions;
        this.executor = executor;
        this.sendTimeoutMs = sendTimeoutMs;
        this.batchSize = batchSize;
        if (sendTimeoutMs <= 0 || batchSize <= 0)
            throw new IllegalArgumentException("Outbox settings must be positive");
    }

    @TransactionalEventListener
    public void afterCommit(OutboxReady event) {
        try {
            executor.execute(() -> publish(event.id()));
        } catch (RuntimeException e) {
            log.warn("Outbox dispatch rejected for {}; Quartz will retry", event.id(), e);
        }
    }

    public void publish(long id) {
        try {
            transactions.executeWithoutResult(
                    status -> {
                        var rows =
                                jdbc.queryForList(
                                        "SELECT topic, order_id, payload FROM saga_outbox WHERE id"
                                            + " = ? FOR UPDATE",
                                        id);
                        if (rows.isEmpty()) return;
                        var row = rows.getFirst();
                        try {
                            kafka.send(
                                            (String) row.get("topic"),
                                            (String) row.get("order_id"),
                                            mapper.readTree((String) row.get("payload")))
                                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                            jdbc.update("DELETE FROM saga_outbox WHERE id = ?", id);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Outbox publisher interrupted", e);
                        } catch (Exception e) {
                            throw new IllegalStateException("Cannot publish outbox event " + id, e);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Outbox delivery failed for {}; Quartz will retry", id, e);
        }
    }

    public long retryBatch(long cursor) {
        List<Long> ids =
                jdbc.queryForList(
                        "SELECT id FROM saga_outbox WHERE id > ? ORDER BY id LIMIT ?",
                        Long.class,
                        cursor,
                        batchSize);
        for (long id : ids) publish(id);
        return ids.isEmpty() || ids.size() < batchSize ? 0 : ids.getLast();
    }
}

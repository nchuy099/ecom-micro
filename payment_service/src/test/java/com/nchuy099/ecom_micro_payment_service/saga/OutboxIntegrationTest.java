package com.nchuy099.ecom_micro_payment_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private SagaOutbox outbox;
    private OutboxPublisher publisher;
    private KafkaTemplate<String, Object> kafka;

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:outbox;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        JdbcTemplate jdbc(DataSource ds) {
            return new JdbcTemplate(ds);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }

        @Bean
        TransactionTemplate transactions(PlatformTransactionManager manager) {
            return new TransactionTemplate(manager);
        }

        @Bean
        ObjectMapper mapper() {
            return new ObjectMapper();
        }

        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafka() {
            return mock(KafkaTemplate.class);
        }

        @Bean
        SagaOutbox outbox(
                JdbcTemplate jdbc,
                ObjectMapper mapper,
                org.springframework.context.ApplicationEventPublisher events) {
            return new SagaOutbox(jdbc, mapper, events);
        }

        @Bean
        OutboxPublisher publisher(
                JdbcTemplate jdbc,
                KafkaTemplate<String, Object> kafka,
                ObjectMapper mapper,
                PlatformTransactionManager manager) {
            var tx = new TransactionTemplate(manager);
            tx.setPropagationBehavior(
                    org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return new OutboxPublisher(jdbc, kafka, mapper, tx, new SyncTaskExecutor(), 1000, 2);
        }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = context.getBean(JdbcTemplate.class);
        tx = context.getBean(TransactionTemplate.class);
        outbox = context.getBean(SagaOutbox.class);
        publisher = context.getBean(OutboxPublisher.class);
        kafka = context.getBean(KafkaTemplate.class);
        jdbc.execute("DROP TABLE IF EXISTS saga_outbox");
        jdbc.execute(
                "CREATE TABLE saga_outbox (id BIGINT AUTO_INCREMENT PRIMARY KEY, topic"
                    + " VARCHAR(100), order_id VARCHAR(36), payload CLOB)");
    }

    @AfterEach
    void close() {
        context.close();
    }

    private int count() {
        return jdbc.queryForObject("SELECT count(*) FROM saga_outbox", Integer.class);
    }

    @Test
    void commitPublishesAndRollbackDoesNot() {
        when(kafka.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        tx.executeWithoutResult(
                status -> {
                    outbox.add("topic", "order", java.util.Map.of("orderId", "order"));
                    verify(kafka, never()).send(anyString(), anyString(), any());
                });
        verify(kafka, times(1)).send(eq("topic"), eq("order"), any());
        assertEquals(0, count());
        tx.executeWithoutResult(
                status -> {
                    outbox.add("topic", "rollback", java.util.Map.of());
                    status.setRollbackOnly();
                });
        verify(kafka, times(1)).send(anyString(), anyString(), any());
        assertEquals(0, count());
    }

    @Test
    void failureIsRetriedAndMissingRowIsIgnored() {
        when(kafka.send(anyString(), anyString(), any()))
                .thenReturn(
                        CompletableFuture.failedFuture(new RuntimeException("offline")),
                        CompletableFuture.completedFuture(null));
        tx.executeWithoutResult(status -> outbox.add("topic", "order", java.util.Map.of()));
        assertEquals(1, count());
        long id = jdbc.queryForObject("SELECT id FROM saga_outbox", Long.class);
        assertEquals(0, publisher.retryBatch(0));
        assertEquals(0, count());
        publisher.publish(id);
        verify(kafka, times(2)).send(anyString(), anyString(), any());
    }

    @Test
    void concurrentPublishersSendOnce() throws Exception {
        jdbc.update(
                "INSERT INTO saga_outbox(topic, order_id, payload) VALUES (?, ?, ?)",
                "topic",
                "order",
                "{}");
        long id = jdbc.queryForObject("SELECT id FROM saga_outbox", Long.class);
        var started = new java.util.concurrent.CountDownLatch(1);
        var result =
                new CompletableFuture<
                        org.springframework.kafka.support.SendResult<String, Object>>();
        when(kafka.send(anyString(), anyString(), any()))
                .thenAnswer(
                        invocation -> {
                            started.countDown();
                            return result;
                        });
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> publisher.publish(id));
            assertTrue(started.await(3, java.util.concurrent.TimeUnit.SECONDS));
            var second = workers.submit(() -> publisher.publish(id));
            result.complete(null);
            first.get(3, java.util.concurrent.TimeUnit.SECONDS);
            second.get(3, java.util.concurrent.TimeUnit.SECONDS);
            verify(kafka, times(1)).send(anyString(), anyString(), any());
            assertEquals(0, count());
        } finally {
            workers.shutdownNow();
        }
    }
}

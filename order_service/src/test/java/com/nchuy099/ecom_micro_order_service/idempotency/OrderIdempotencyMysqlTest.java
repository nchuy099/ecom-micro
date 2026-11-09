package com.nchuy099.ecom_micro_order_service.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderItemReq;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;
import com.nchuy099.ecom_micro_order_service.service.OrderService;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "IDEMPOTENCY_TEST_DB_URL", matches = ".+")
class OrderIdempotencyMysqlTest {
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    System.getenv("IDEMPOTENCY_TEST_DB_URL"), "root", "password");
        }

        @Bean
        JdbcTemplate jdbc(DataSource ds) {
            return new JdbcTemplate(ds);
        }

        @Bean
        PlatformTransactionManager tx(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }

        @Bean
        ObjectMapper mapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        OrderService orders() {
            return mock(OrderService.class);
        }

        @Bean
        OrderIdempotencyService idempotency(
                JdbcTemplate jdbc, OrderService orders, ObjectMapper mapper) {
            return new OrderIdempotencyService(jdbc, orders, mapper);
        }
    }

    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private OrderIdempotencyService service;
    private OrderService orders;

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = context.getBean(JdbcTemplate.class);
        service = context.getBean(OrderIdempotencyService.class);
        orders = context.getBean(OrderService.class);
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS order_idempotency (user_id VARCHAR(255) NOT NULL,"
                    + " idempotency_key CHAR(36) NOT NULL, request_hash CHAR(64) NOT NULL, status"
                    + " VARCHAR(20) NOT NULL, http_status INT, response_body LONGTEXT, expires_at"
                    + " TIMESTAMP(6), PRIMARY KEY(user_id,idempotency_key))");
        jdbc.execute("DELETE FROM order_idempotency");
    }

    @AfterEach
    void close() {
        context.close();
    }

    private CreateOrderReq request(int quantity) {
        var req = new CreateOrderReq();
        req.setCustomerId("customer");
        var item = new CreateOrderItemReq();
        item.setProductId("product");
        item.setQuantity(quantity);
        req.setItems(List.of(item));
        return req;
    }

    @Test
    void replayConflictExpirationAndRollback() throws Exception {
        when(orders.create(any()))
                .thenReturn(
                        OrderEntity.builder().id("one").build(),
                        OrderEntity.builder().id("two").build());
        var first = service.create("user", "key", request(1));
        assertEquals(first, service.create("user", "key", request(1)));
        assertThrows(
                OrderIdempotencyService.IdempotencyConflictException.class,
                () -> service.create("user", "key", request(2)));
        verify(orders, times(1)).create(any());
        jdbc.update(
                "UPDATE order_idempotency SET expires_at = CURRENT_TIMESTAMP(6) - INTERVAL 1"
                    + " SECOND");
        assertNotEquals(first.body(), service.create("user", "key", request(2)).body());
        when(orders.create(any())).thenThrow(new IllegalStateException("failure"));
        assertThrows(
                IllegalStateException.class, () -> service.create("user", "other", request(1)));
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT count(*) FROM order_idempotency WHERE idempotency_key='other'",
                        Integer.class));
    }

    @Test
    void concurrentRequestsWaitAndReplay() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(orders.create(any()))
                .thenAnswer(
                        i -> {
                            entered.countDown();
                            release.await(4, TimeUnit.SECONDS);
                            return OrderEntity.builder().id("one").build();
                        });
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> service.create("user", "key", request(1)));
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            var b = pool.submit(() -> service.create("user", "key", request(1)));
            release.countDown();
            assertEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
            verify(orders, times(1)).create(any());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void competingRequestTimesOutThenCanRetry() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(orders.create(any()))
                .thenAnswer(
                        i -> {
                            entered.countDown();
                            release.await(12, TimeUnit.SECONDS);
                            return OrderEntity.builder().id("one").build();
                        });
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> service.create("user", "key", request(1)));
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            var b = pool.submit(() -> service.create("user", "key", request(1)));
            var failure = assertThrows(ExecutionException.class, () -> b.get(8, TimeUnit.SECONDS));
            assertInstanceOf(
                    org.springframework.dao.PessimisticLockingFailureException.class,
                    failure.getCause());
            release.countDown();
            assertEquals(a.get(3, TimeUnit.SECONDS), service.create("user", "key", request(1)));
            verify(orders, times(1)).create(any());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }
}

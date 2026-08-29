package com.nchuy099.ecommerce.order;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.nchuy099.ecommerce.order.client.ProductClient;
import com.nchuy099.ecommerce.order.client.dto.ProductClientResponse;
import com.nchuy099.ecommerce.order.exception.ProductReservationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_resilience;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "eureka.client.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "ecommerce.kafka.topics.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductClientResilienceIntegrationTest {

    private static HttpServer server;
    private static final AtomicInteger responseMode = new AtomicInteger(0);
    private static final AtomicInteger callCount = new AtomicInteger(0);

    private static final int MODE_RETRY_THEN_SUCCESS = 1;
    private static final int MODE_CONFLICT_409 = 2;
    private static final int MODE_SERVER_ERROR_500 = 3;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/products/1/reserve", ProductClientResilienceIntegrationTest::handleReserve);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("services.product.url", () -> "http://localhost:" + server.getAddress().getPort());
        registry.add("services.product.connect-timeout-ms", () -> 2000);
        registry.add("services.product.read-timeout-ms", () -> 3000);
    }

    @BeforeEach
    void resetState() {
        callCount.set(0);
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productService");
        cb.reset();
    }

    private static void handleReserve(HttpExchange exchange) throws IOException {
        int count = callCount.incrementAndGet();
        int mode = responseMode.get();

        if (mode == MODE_RETRY_THEN_SUCCESS) {
            if (count < 3) {
                sendResponse(exchange, 503, "application/problem+json",
                        "{\"type\":\"about:blank\",\"title\":\"Service Unavailable\",\"status\":503,\"detail\":\"Temporary glitch\"}");
            } else {
                sendResponse(exchange, 200, "application/json",
                        "{\"data\":{\"id\":1,\"name\":\"Phone\",\"sku\":\"PH-1\",\"price\":500.00,\"stock\":5,\"categoryId\":1,\"version\":1}}");
            }
        } else if (mode == MODE_CONFLICT_409) {
            sendResponse(exchange, 409, "application/problem+json",
                    "{\"type\":\"https://errors.ecom.local/insufficient-stock\",\"title\":\"Insufficient stock\",\"status\":409,\"detail\":\"Insufficient stock for product 1\"}");
        } else if (mode == MODE_SERVER_ERROR_500) {
            sendResponse(exchange, 500, "application/problem+json",
                    "{\"type\":\"about:blank\",\"title\":\"Internal Server Error\",\"status\":500,\"detail\":\"Fatal database crash\"}");
        } else {
            sendResponse(exchange, 200, "application/json",
                    "{\"data\":{\"id\":1,\"name\":\"Phone\",\"sku\":\"PH-1\",\"price\":500.00,\"stock\":5,\"categoryId\":1,\"version\":1}}");
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void retriesOnTransient503AndSucceedsOnThirdAttempt() {
        responseMode.set(MODE_RETRY_THEN_SUCCESS);

        ProductClientResponse response = productClient.reserveStock(1L, 2);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(callCount.get()).isEqualTo(3);

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productService");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void doesNotRetryOn409ConflictAndDoesNotRecordAsCircuitFailure() {
        responseMode.set(MODE_CONFLICT_409);

        assertThatThrownBy(() -> productClient.reserveStock(1L, 2))
                .isInstanceOf(ProductReservationException.class)
                .satisfies(ex -> assertThat(((ProductReservationException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        // 409 Conflict must fail immediately with exactly 1 call
        assertThat(callCount.get()).isEqualTo(1);

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productService");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(cb.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void circuitBreakerOpensAfterFailureThresholdAndFailsFastWithoutNetworkCalls() {
        responseMode.set(MODE_SERVER_ERROR_500);
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productService");

        // Execute calls until minimum-number-of-calls (3) is reached and circuit trips
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> productClient.reserveStock(1L, 2))
                    .isInstanceOf(ProductReservationException.class);
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        int callsRecordedSoFar = callCount.get();

        // When circuit is OPEN, next call must fail fast with CallNotPermittedException
        // and must NOT make any additional network calls
        assertThatThrownBy(() -> productClient.reserveStock(1L, 2))
                .isInstanceOf(CallNotPermittedException.class);

        assertThat(callCount.get()).isEqualTo(callsRecordedSoFar);
    }

    @Test
    void circuitBreakerStateIsObservable() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("productService");
        assertThat(cb).isNotNull();
        assertThat(cb.getName()).isEqualTo("productService");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}

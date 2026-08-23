package com.nchuy099.ecommerce.gateway;

import com.nchuy099.ecommerce.gateway.ratelimit.RateLimitResult;
import com.nchuy099.ecommerce.gateway.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=api-gateway-test",
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false"
        }
)
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @BeforeEach
    void allowRateLimitedRequests() {
        when(tokenBucketRateLimiter.consume(anyString(), anyInt(), anyLong()))
                .thenReturn(Mono.just(RateLimitResult.allowed(9)));
    }

    @Test
    void unauthenticatedOrderCreateReturns401() {
        webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"userId\":100,\"items\":[{\"productId\":1,\"quantity\":1}]}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void sellerCannotCreateOrder() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SELLER"))
                        .jwt(jwt -> jwt.claim("user_id", "200")))
                .post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"userId\":200,\"items\":[{\"productId\":1,\"quantity\":1}]}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void customerCanCreateOrder() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                        .jwt(jwt -> jwt.claim("user_id", "100")))
                .post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"userId\":100,\"items\":[{\"productId\":1,\"quantity\":1}]}")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void unauthenticatedFlashSalePurchaseReturns401() {
        webTestClient.post()
                .uri("/api/flashsale/1/purchase")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void sellerCannotPurchaseFlashSale() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SELLER"))
                        .jwt(jwt -> jwt.claim("user_id", "200")))
                .post()
                .uri("/api/flashsale/1/purchase")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void customerCanPurchaseFlashSale() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                        .jwt(jwt -> jwt.claim("user_id", "100")))
                .post()
                .uri("/api/flashsale/1/purchase")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void customerCannotCreateProduct() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                        .jwt(jwt -> jwt.claim("user_id", "100")))
                .post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Test\",\"price\":10,\"stock\":5}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void sellerCanCreateProduct() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_SELLER"))
                        .jwt(jwt -> jwt.claim("user_id", "200")))
                .post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Test\",\"price\":10,\"stock\":5}")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void customerCannotManageUsers() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                        .jwt(jwt -> jwt.claim("user_id", "100")))
                .post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"new\",\"email\":\"new@example.com\",\"role\":\"ROLE_CUSTOMER\",\"tier\":\"STANDARD\"}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminCanManageUsers() {
        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.claim("user_id", "1")))
                .post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"new\",\"email\":\"new@example.com\",\"role\":\"ROLE_CUSTOMER\",\"tier\":\"STANDARD\"}")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void actuatorHealthIsAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        @Primary
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.error(new JwtException("Tests use mockJwt()"));
        }
    }
}

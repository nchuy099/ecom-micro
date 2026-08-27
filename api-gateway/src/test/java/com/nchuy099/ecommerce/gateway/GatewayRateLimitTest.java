package com.nchuy099.ecommerce.gateway;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import com.nchuy099.ecommerce.gateway.ratelimit.RateLimitGatewayFilter;
import com.nchuy099.ecommerce.gateway.ratelimit.RateLimitResult;
import com.nchuy099.ecommerce.gateway.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=api-gateway-rate-limit-test",
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false"
        }
)
@AutoConfigureWebTestClient
class GatewayRateLimitTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @Test
    void loginRateLimitReturns429WithHeadersWithoutForwarding() {
        when(tokenBucketRateLimiter.consume(startsWith("rl:login:ip:"), anyInt(), anyLong()))
                .thenReturn(Mono.just(RateLimitResult.rejected(12)));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=password&client_id=ecom-gateway")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "12")
                .expectHeader().valueEquals(RateLimitGatewayFilter.RATE_LIMIT_REMAINING_HEADER, "0");

        verify(tokenBucketRateLimiter).consume(startsWith("rl:login:ip:"), anyInt(), anyLong());
    }

    @Test
    void orderRateLimitUsesAuthenticatedUserAndReturns429BeforeRouting() {
        when(tokenBucketRateLimiter.consume(startsWith("rl:orders:user:100"), anyInt(), anyLong()))
                .thenReturn(Mono.just(RateLimitResult.rejected(1)));

        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                        .jwt(jwt -> jwt.claim("user_id", "100")))
                .post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"userId\":100,\"items\":[{\"productId\":1,\"quantity\":1}]}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "1")
                .expectHeader().valueEquals(RateLimitGatewayFilter.RATE_LIMIT_REMAINING_HEADER, "0");
    }

    @Test
    void flashSaleRouteIsRateLimitedBeforeRouting() {
        when(tokenBucketRateLimiter.consume(startsWith("rl:flashsale:user:100"), anyInt(), anyLong()))
                .thenReturn(Mono.just(RateLimitResult.rejected(2)));

        webTestClient.mutateWith(mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                        .jwt(jwt -> jwt.claim("user_id", "100")))
                .post()
                .uri("/api/flashsale/1/purchase")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "2")
                .expectHeader().valueEquals(RateLimitGatewayFilter.RATE_LIMIT_REMAINING_HEADER, "0");
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

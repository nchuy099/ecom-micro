package com.nchuy099.ecom_micro_api_gateway.ratelimit;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RATE_LIMIT_TEST_REDIS", matches = "true")
class OrderRateLimitRedisTest {
    @Test
    void tokenBucketIsSharedAndIsolatedPerUser() {
        var factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        factory.start();
        try {
            var redis = new ReactiveStringRedisTemplate(factory);
            var first = new OrderRateLimitFilter(redis, 2, 60000);
            var second = new OrderRateLimitFilter(redis, 2, 60000);
            String user = UUID.randomUUID().toString();
            assertEquals(HttpStatus.OK, request(first, user));
            assertEquals(HttpStatus.OK, request(second, user));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, request(first, user));
            assertEquals(HttpStatus.OK, request(second, UUID.randomUUID().toString()));
            redis.delete("rate_limit:order:" + user).block();
        } finally {
            factory.destroy();
        }
    }

    private HttpStatus request(OrderRateLimitFilter filter, String user) {
        var jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject(user)
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build();
        var exchange =
                MockServerWebExchange.builder(MockServerHttpRequest.post("/v1/orders"))
                        .principal(new JwtAuthenticationToken(jwt))
                        .build();
        filter.filter(
                        exchange,
                        e -> {
                            e.getResponse().setStatusCode(HttpStatus.OK);
                            return e.getResponse().setComplete();
                        })
                .block();
        return (HttpStatus) exchange.getResponse().getStatusCode();
    }
}

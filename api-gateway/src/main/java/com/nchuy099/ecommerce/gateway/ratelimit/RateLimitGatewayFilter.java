package com.nchuy099.ecommerce.gateway.ratelimit;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitGatewayFilter implements WebFilter, Ordered {
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";


    private final RateLimitProperties properties;
    private final TokenBucketRateLimiter rateLimiter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        RateLimitPolicyMatch match = match(exchange.getRequest());
        if (match == null || !match.policy().isEnabled()) {
            return chain.filter(exchange);
        }

        return callerKey(exchange, match)
                .flatMap(key -> rateLimiter.consume(
                                key,
                                match.policy().getCapacity(),
                                refillSeconds(match.policy().getRefillPeriod()))
                        .onErrorResume(ex -> {
                            log.warn("Rate limiter failed for key={}, allowing request: {}", key, ex.getMessage(), ex);
                            return Mono.just(RateLimitResult.allowed(match.policy().getCapacity() - 1L));
                        }))
                .flatMap(result -> {
                    exchange.getResponse().getHeaders()
                            .set(RATE_LIMIT_REMAINING_HEADER, String.valueOf(result.remaining()));
                    if (result.allowed()) {
                        return chain.filter(exchange);
                    }
                    return reject(exchange, result);
                });
    }

    private RateLimitPolicyMatch match(ServerHttpRequest request) {
        String path = request.getPath().pathWithinApplication().value();
        HttpMethod method = request.getMethod();

        if (HttpMethod.POST.equals(method) && "/api/auth/login".equals(path)) {
            return new RateLimitPolicyMatch("login", properties.getLogin(), false);
        }
        if (HttpMethod.POST.equals(method) && "/api/orders".equals(path)) {
            return new RateLimitPolicyMatch("orders", properties.getOrders(), true);
        }
        if (path.startsWith("/api/flashsale/")) {
            return new RateLimitPolicyMatch("flashsale", properties.getFlashSale(), true);
        }
        return null;
    }

    private Mono<String> callerKey(ServerWebExchange exchange, RateLimitPolicyMatch match) {
        Mono<String> userKey = exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(this::jwtUserId)
                .filter(value -> !value.isBlank())
                .map(userId -> "rl:" + match.name() + ":user:" + userId);

        Mono<String> ipKey = Mono.fromSupplier(() -> "rl:" + match.name() + ":ip:" + clientIp(exchange.getRequest()));
        return match.preferUser() ? userKey.switchIfEmpty(ipKey) : ipKey;
    }

    private String jwtUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        return jwt.getSubject() == null ? "" : jwt.getSubject();
    }

    private String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() == null
                ? "unknown"
                : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private long refillSeconds(Duration duration) {
        return Math.max(1, duration.toSeconds());
    }

    private Mono<Void> reject(ServerWebExchange exchange, RateLimitResult result) {
        byte[] body = """
                {"type":"https://errors.ecom.local/rate-limit-exceeded","title":"Rate limit exceeded","status":429,"detail":"Too many requests"}
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
        exchange.getResponse().getHeaders().set(RATE_LIMIT_REMAINING_HEADER, "0");
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    private record RateLimitPolicyMatch(
            String name,
            RateLimitProperties.Policy policy,
            boolean preferUser
    ) {
    }
}

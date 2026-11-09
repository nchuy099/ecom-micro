package com.nchuy099.ecom_micro_api_gateway.ratelimit;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class OrderRateLimitFilter implements GlobalFilter, Ordered {
    // Redis server time keeps all gateway instances on the same clock.
    private static final String LUA =
            """
            local now = redis.call('TIME')
            local ms = now[1] * 1000 + math.floor(now[2] / 1000)
            local capacity = tonumber(ARGV[1])
            local interval = tonumber(ARGV[2])
            local values = redis.call('HMGET', KEYS[1], 'tokens', 'time')
            local tokens = tonumber(values[1]) or capacity
            local last = tonumber(values[2]) or ms
            tokens = math.min(capacity, tokens + math.max(0, ms - last) / interval)
            local allowed = 0
            local retry = 0
            if tokens >= 1 then tokens = tokens - 1; allowed = 1
            else retry = math.ceil((1 - tokens) * interval / 1000) end
            redis.call('HSET', KEYS[1], 'tokens', tokens, 'time', ms)
            redis.call('PEXPIRE', KEYS[1], math.ceil(capacity * interval))
            return {allowed, retry}
            """;
    private final ReactiveStringRedisTemplate redis;
    private final int capacity;
    private final long refillMs;
    private final DefaultRedisScript<List> script = new DefaultRedisScript<>(LUA, List.class);

    public OrderRateLimitFilter(
            ReactiveStringRedisTemplate redis,
            @Value("${order.rate-limit.capacity:10}") int capacity,
            @Value("${order.rate-limit.refill-ms:6000}") long refillMs) {
        if (capacity < 1 || refillMs < 1)
            throw new IllegalArgumentException("Rate limit settings must be positive");
        this.redis = redis;
        this.capacity = capacity;
        this.refillMs = refillMs;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getMethod() != HttpMethod.POST
                || !exchange.getRequest().getURI().getPath().matches("/v1/orders/?"))
            return chain.filter(exchange);
        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .switchIfEmpty(Mono.error(new MissingPrincipalException()))
                .flatMap(
                        auth -> {
                            String user = auth.getToken().getSubject();
                            if (user == null || user.isBlank())
                                return respond(exchange, HttpStatus.UNAUTHORIZED, 0);
                            return redis.execute(
                                            script,
                                            List.of("rate_limit:order:" + user),
                                            List.of(
                                                    Integer.toString(capacity),
                                                    Long.toString(refillMs)))
                                    .next()
                                    .map(
                                            result ->
                                                    new Decision(
                                                            ((Number) result.get(0)).longValue()
                                                                    == 1,
                                                            ((Number) result.get(1)).longValue()))
                                    .onErrorResume(ex -> Mono.just(new Decision(false, -1)))
                                    .switchIfEmpty(Mono.just(new Decision(false, -1)))
                                    .flatMap(
                                            decision ->
                                                    decision.retry == -1
                                                            ? respond(
                                                                    exchange,
                                                                    HttpStatus.SERVICE_UNAVAILABLE,
                                                                    0)
                                                            : decision.allowed
                                                                    ? chain.filter(exchange)
                                                                    : respond(
                                                                            exchange,
                                                                            HttpStatus
                                                                                    .TOO_MANY_REQUESTS,
                                                                            decision.retry));
                        })
                .onErrorResume(
                        MissingPrincipalException.class,
                        ex -> respond(exchange, HttpStatus.UNAUTHORIZED, 0));
    }

    private Mono<Void> respond(ServerWebExchange exchange, HttpStatus status, long retry) {
        exchange.getResponse().setStatusCode(status);
        if (retry > 0) exchange.getResponse().getHeaders().set("Retry-After", Long.toString(retry));
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private static class MissingPrincipalException extends RuntimeException {}

    private record Decision(boolean allowed, long retry) {}
}

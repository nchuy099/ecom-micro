package com.nchuy099.ecommerce.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Propagates identity headers to downstream services after successful JWT validation.
 */
@Component
public class IdentityHeaderGatewayFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return resolveJwt(exchange)
                .map(jwt -> enrichRequest(exchange.getRequest(), jwt))
                .map(request -> exchange.mutate().request(request).build())
                .flatMap(chain::filter)
                .switchIfEmpty(chain.filter(exchange));
    }

    static ServerHttpRequest enrichRequest(ServerHttpRequest request, Jwt jwt) {
        return request.mutate()
                .header(USER_ID_HEADER, resolveUserId(jwt))
                .header(TRACE_ID_HEADER, resolveTraceId(request))
                .build();
    }

    private Mono<Jwt> resolveJwt(ServerWebExchange exchange) {
        Mono<Jwt> fromExchange = exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken);

        Mono<Jwt> fromSecurityContext = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken);

        return fromExchange.switchIfEmpty(fromSecurityContext);
    }

    static String resolveUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        return jwt.getSubject();
    }

    static String resolveTraceId(ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

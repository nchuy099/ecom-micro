package com.nchuy099.ecommerce.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityHeaderGatewayFilterTest {

    private final IdentityHeaderGatewayFilter filter = new IdentityHeaderGatewayFilter();

    @Test
    void enrichRequestUsesUserIdClaimAndIncomingTraceId() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(IdentityHeaderGatewayFilter.TRACE_ID_HEADER, "trace-abc")
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("keycloak-subject")
                .claim("user_id", "100")
                .build();

        var enriched = IdentityHeaderGatewayFilter.enrichRequest(request, jwt);

        assertThat(enriched.getHeaders().getFirst(IdentityHeaderGatewayFilter.USER_ID_HEADER)).isEqualTo("100");
        assertThat(enriched.getHeaders().getFirst(IdentityHeaderGatewayFilter.TRACE_ID_HEADER)).isEqualTo("trace-abc");
    }

    @Test
    void enrichRequestFallsBackToSubjectAndGeneratesTraceId() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders").build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("fallback-subject")
                .build();

        var enriched = IdentityHeaderGatewayFilter.enrichRequest(request, jwt);

        assertThat(enriched.getHeaders().getFirst(IdentityHeaderGatewayFilter.USER_ID_HEADER))
                .isEqualTo("fallback-subject");
        assertThat(enriched.getHeaders().getFirst(IdentityHeaderGatewayFilter.TRACE_ID_HEADER))
                .isNotBlank();
    }

    @Test
    void skipsHeadersWhenUnauthenticated() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getRequest().getHeaders().getFirst(IdentityHeaderGatewayFilter.USER_ID_HEADER)).isNull();
        assertThat(exchange.getRequest().getHeaders().getFirst(IdentityHeaderGatewayFilter.TRACE_ID_HEADER)).isNull();
    }
}

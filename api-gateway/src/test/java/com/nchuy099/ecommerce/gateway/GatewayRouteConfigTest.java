package com.nchuy099.ecommerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=api-gateway-test",
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false"
        }
)
@Import(GatewayRouteConfigTest.JwtTestConfig.class)
class GatewayRouteConfigTest {

    @LocalServerPort
    private int port;

    @Autowired
    private GatewayProperties gatewayProperties;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Gateway context starts with Eureka client offline
    }

    @Test
    void userServiceRouteIsConfiguredCorrectly() {
        RouteDefinition route = findRoute("user-service");
        assertThat(route.getUri().toString()).isEqualTo("lb://user-service");
        assertThat(pathPattern(route)).isEqualTo("/api/users/**");
        assertThat(rewriteRegexp(route)).isEqualTo("/api/users(?<segment>/?.*)");
        assertThat(rewriteReplacement(route)).isEqualTo("/v1/users$\\{segment}");
    }

    @Test
    void productServiceRouteIsConfiguredCorrectly() {
        RouteDefinition route = findRoute("product-service");
        assertThat(route.getUri().toString()).isEqualTo("lb://product-service");
        assertThat(pathPattern(route)).isEqualTo("/api/products/**");
        assertThat(rewriteRegexp(route)).isEqualTo("/api/products(?<segment>/?.*)");
        assertThat(rewriteReplacement(route)).isEqualTo("/v1/products$\\{segment}");
    }

    @Test
    void orderServiceRouteIsConfiguredCorrectly() {
        RouteDefinition route = findRoute("order-service");
        assertThat(route.getUri().toString()).isEqualTo("lb://order-service");
        assertThat(pathPattern(route)).isEqualTo("/api/orders/**");
        assertThat(rewriteRegexp(route)).isEqualTo("/api/orders(?<segment>/?.*)");
        assertThat(rewriteReplacement(route)).isEqualTo("/v1/orders$\\{segment}");
    }

    @Test
    void flashSaleRouteIsConfiguredCorrectly() {
        RouteDefinition route = findRoute("flashsale-service");
        assertThat(route.getUri().toString()).isEqualTo("lb://order-service");
        assertThat(pathPattern(route)).isEqualTo("/api/flashsale/**");
        assertThat(rewriteRegexp(route)).isEqualTo("/api/flashsale(?<segment>/?.*)");
        assertThat(rewriteReplacement(route)).isEqualTo("/v1/flashsale$\\{segment}");
    }

    @Test
    void keycloakLoginRouteIsConfiguredCorrectly() {
        RouteDefinition route = findRoute("keycloak-login");
        assertThat(route.getUri().toString()).isEqualTo("http://localhost:8090");
        assertThat(pathPattern(route)).isEqualTo("/api/auth/login");
        assertThat(rewriteRegexp(route)).isEqualTo("/api/auth/login");
        assertThat(rewriteReplacement(route)).isEqualTo("/realms/ecom/protocol/openid-connect/token");
    }

    @Test
    void authServiceRouteIsAbsent() {
        // EC-P1-002 proxies Keycloak login directly; auth-service remains out of the request path.
        assertThat(gatewayProperties.getRoutes())
                .noneMatch(r -> "auth-service".equals(r.getId()));
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        @Primary
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.error(new JwtException("Tests do not require a live JWT decoder"));
        }
    }

    @Test
    void actuatorHealthIsAccessibleWithoutAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RouteDefinition findRoute(String id) {
        return gatewayProperties.getRoutes().stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Route not found: " + id));
    }

    /** Reads the Path predicate pattern (shortcut arg key _genkey_0). */
    private String pathPattern(RouteDefinition route) {
        return route.getPredicates().stream()
                .filter(p -> "Path".equals(p.getName()))
                .findFirst()
                .map(p -> p.getArgs().get("_genkey_0"))
                .orElseThrow(() -> new AssertionError("Path predicate not found on route: " + route.getId()));
    }

    private String rewriteRegexp(RouteDefinition route) {
        return rewriteArgs(route).get("regexp");
    }

    private String rewriteReplacement(RouteDefinition route) {
        return rewriteArgs(route).get("replacement");
    }

    /**
     * Returns the args map of the RewritePath filter.
     * Structured YAML (name/args form) stores keys as "regexp" and "replacement".
     * The stored replacement value uses $\{segment} (dollar + backslash + brace),
     * which RewritePathGatewayFilterFactory converts to ${segment} at runtime
     * via replacement.replace("$\\", "$").
     */
    private Map<String, String> rewriteArgs(RouteDefinition route) {
        return route.getFilters().stream()
                .filter(f -> "RewritePath".equals(f.getName()))
                .findFirst()
                .map(FilterDefinition::getArgs)
                .orElseThrow(() -> new AssertionError("RewritePath filter not found on route: " + route.getId()));
    }
}

package com.nchuy099.ecommerce.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.application.name=api-gateway-test",
                "eureka.client.enabled=false"
        }
)
@Import(GatewayRouteConfigTest.JwtTestConfig.class)
class GatewayRouteConfigTest {

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @Test
    void extractsRealmRolesFromJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .claim("realm_access", Map.of("roles", List.of("ROLE_CUSTOMER", "ROLE_VIP")))
                .build();

        var authorities = jwtAuthConverter.extractAuthorities(jwt);

        assertThat(authorities)
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_VIP");
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        @Primary
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.error(new JwtException("Tests do not require a live JWT decoder"));
        }
    }
}

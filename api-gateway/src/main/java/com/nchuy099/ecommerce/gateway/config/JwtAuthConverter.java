package com.nchuy099.ecommerce.gateway.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

/**
 * Maps Keycloak JWT claims to Spring Security authorities.
 * <p>
 * Supports both the standard {@code realm_access.roles} structure and the flat
 * {@code realm_access.roles} claim produced by the realm protocol mapper.
 */
@Component
public class JwtAuthConverter {

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    public ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    private Converter<Jwt, Flux<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> Flux.fromIterable(extractAuthorities(jwt));
    }

    Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object realmRoles = realmAccess.get("roles");
            if (realmRoles instanceof Collection<?> collection) {
                collection.forEach(role -> roles.add(String.valueOf(role)));
            }
        }

        if (jwt.hasClaim("realm_access.roles")) {
            Object flatRoles = jwt.getClaim("realm_access.roles");
            if (flatRoles instanceof Collection<?> collection) {
                collection.forEach(role -> roles.add(String.valueOf(role)));
            } else if (flatRoles instanceof String role) {
                roles.add(role);
            }
        }

        scopeConverter.convert(jwt).forEach(authority -> roles.add(authority.getAuthority()));

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            authorities.add(new SimpleGrantedAuthority(normalized));
        }
        return authorities;
    }
}

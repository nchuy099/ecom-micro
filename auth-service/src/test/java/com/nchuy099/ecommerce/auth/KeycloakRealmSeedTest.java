package com.nchuy099.ecommerce.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmSeedTest {

    private static final Set<String> REQUIRED_ROLES = Set.of(
            "ROLE_CUSTOMER",
            "ROLE_VIP",
            "ROLE_SELLER",
            "ROLE_ADMIN"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Path realmSeed() {
        Path direct = Path.of("docker/keycloak/ecom-realm.json");
        if (direct.toFile().exists()) {
            return direct;
        }
        return Path.of("../docker/keycloak/ecom-realm.json").normalize();
    }

    @Test
    void realmSeedDefinesEcomRealm() throws Exception {
        JsonNode root = objectMapper.readTree(realmSeed().toFile());

        assertThat(root.get("realm").asText()).isEqualTo("ecom");
        assertThat(root.get("enabled").asBoolean()).isTrue();
    }

    @Test
    void realmSeedDefinesGatewayClient() throws Exception {
        JsonNode root = objectMapper.readTree(realmSeed().toFile());

        JsonNode client = StreamSupport.stream(root.get("clients").spliterator(), false)
                .filter(node -> "ecom-gateway".equals(node.get("clientId").asText()))
                .findFirst()
                .orElseThrow();

        assertThat(client.get("enabled").asBoolean()).isTrue();
        assertThat(client.get("publicClient").asBoolean()).isTrue();
        assertThat(client.get("directAccessGrantsEnabled").asBoolean()).isTrue();
    }

    @Test
    void realmSeedDefinesRequiredRoles() throws Exception {
        JsonNode root = objectMapper.readTree(realmSeed().toFile());

        Set<String> roles = StreamSupport.stream(root.get("roles").get("realm").spliterator(), false)
                .map(node -> node.get("name").asText())
                .collect(Collectors.toSet());

        assertThat(roles).containsAll(REQUIRED_ROLES);
    }

    @Test
    void realmSeedDefinesUserIdProtocolMapper() throws Exception {
        JsonNode root = objectMapper.readTree(realmSeed().toFile());

        JsonNode client = StreamSupport.stream(root.get("clients").spliterator(), false)
                .filter(node -> "ecom-gateway".equals(node.get("clientId").asText()))
                .findFirst()
                .orElseThrow();

        boolean hasUserIdMapper = StreamSupport.stream(client.get("protocolMappers").spliterator(), false)
                .anyMatch(mapper -> "user_id".equals(mapper.get("name").asText()));

        assertThat(hasUserIdMapper).isTrue();
    }
}

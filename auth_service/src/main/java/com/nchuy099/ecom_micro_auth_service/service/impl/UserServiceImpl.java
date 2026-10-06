package com.nchuy099.ecom_micro_auth_service.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import com.nchuy099.ecom_micro_auth_service.dto.request.LoginRequest;
import com.nchuy099.ecom_micro_auth_service.dto.request.UserRegistrationDto;
import com.nchuy099.ecom_micro_auth_service.dto.response.TokenResponse;
import com.nchuy099.ecom_micro_auth_service.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final Keycloak keycloak;
    private final RestClient restClient = RestClient.create();

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Override
    public void createUser(UserRegistrationDto dto) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        user.setCredentials(List.of(credential));

        try (var response = realmResource().users().create(user)) {
            if (response.getStatus() != 201) {
                throw new IllegalStateException("Create user failed with status " + response.getStatus());
            }
        }
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.set("grant_type", "password");
        form.set("client_id", clientId);
        form.set("client_secret", clientSecret);
        form.set("username", request.getUsername());
        form.set("password", request.getPassword());

        return restClient.post()
                .uri(authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }
}

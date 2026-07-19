package com.nchuy099.ecommerce.user;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.user.controller.UserController;
import com.nchuy099.ecommerce.user.dto.CreateUserRequest;
import com.nchuy099.ecommerce.user.dto.PageResponse;
import com.nchuy099.ecommerce.user.dto.UpdateUserRequest;
import com.nchuy099.ecommerce.user.dto.UserResponse;
import com.nchuy099.ecommerce.user.dto.UserSearchRequest;
import com.nchuy099.ecommerce.user.entity.UserRole;
import com.nchuy099.ecommerce.user.entity.UserTier;
import com.nchuy099.ecommerce.user.exception.DuplicateUserException;
import com.nchuy099.ecommerce.user.exception.UserNotFoundException;
import com.nchuy099.ecommerce.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void createsUser() throws Exception {
        when(userService.create(any())).thenReturn(userResponse(1L, "alice"));

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest(
                                "alice",
                                "alice@example.com",
                                "+84901234567",
                                UserRole.ROLE_CUSTOMER,
                                UserTier.STANDARD
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void readsUpdatesDeletesAndSearches() throws Exception {
        when(userService.findById(1L)).thenReturn(userResponse(1L, "alice"));
        when(userService.update(eq(1L), any())).thenReturn(userResponse(1L, "alice-updated"));
        when(userService.search(any())).thenReturn(PageResponse.of(List.of(userResponse(1L, "alice")), 0, 20, 1));
        doNothing().when(userService).delete(1L);

        mockMvc.perform(get("/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));

        mockMvc.perform(put("/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest(
                                "alice-updated",
                                "alice@example.com",
                                null,
                                UserRole.ROLE_CUSTOMER,
                                UserTier.VIP
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice-updated"));

        mockMvc.perform(post("/v1/users/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserSearchRequest("alice", null, null, 0, 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("alice"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(delete("/v1/users/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void returnsProblemDetailForValidationFailure() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "phone": "abc",
                                  "role": "ROLE_CUSTOMER",
                                  "tier": "STANDARD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void returnsProblemDetailForNotFoundAndDuplicate() throws Exception {
        when(userService.findById(99L)).thenThrow(new UserNotFoundException(99L));
        when(userService.create(any())).thenThrow(new DuplicateUserException());

        mockMvc.perform(get("/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"));

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest(
                                "alice",
                                "alice@example.com",
                                null,
                                UserRole.ROLE_CUSTOMER,
                                UserTier.STANDARD
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate user"));
    }

    private static UserResponse userResponse(Long id, String username) {
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        return new UserResponse(
                id,
                username,
                username + "@example.com",
                null,
                UserRole.ROLE_CUSTOMER,
                UserTier.STANDARD,
                now,
                now
        );
    }
}

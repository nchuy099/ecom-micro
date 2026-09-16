package com.nchuy099.ecommerce.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nchuy099.ecommerce.user.dto.CreateUserRequest;
import com.nchuy099.ecommerce.user.dto.PageResponse;
import com.nchuy099.ecommerce.user.dto.UpdateUserRequest;
import com.nchuy099.ecommerce.user.dto.UserResponse;
import com.nchuy099.ecommerce.user.dto.UserSearchRequest;
import com.nchuy099.ecommerce.user.entity.UserRole;
import com.nchuy099.ecommerce.user.entity.UserTier;
import com.nchuy099.ecommerce.user.exception.BusinessException;
import com.nchuy099.ecommerce.user.repository.UserRepository;
import com.nchuy099.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "eureka.client.enabled=false"
})
class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void createsAndReadsUser() {
        UserResponse created = userService.create(new CreateUserRequest(
                "alice",
                "alice@example.com",
                "+84901234567",
                UserRole.ROLE_CUSTOMER,
                UserTier.STANDARD
        ));

        UserResponse found = userService.findById(created.id());

        assertThat(found.username()).isEqualTo("alice");
        assertThat(found.email()).isEqualTo("alice@example.com");
        assertThat(found.role()).isEqualTo(UserRole.ROLE_CUSTOMER);
        assertThat(found.createdAt()).isNotNull();
        assertThat(found.updatedAt()).isNotNull();
    }

    @Test
    void updatesAndDeletesUser() {
        UserResponse created = userService.create(new CreateUserRequest(
                "seller",
                "seller@example.com",
                null,
                UserRole.ROLE_SELLER,
                UserTier.STANDARD
        ));

        UserResponse updated = userService.update(created.id(), new UpdateUserRequest(
                "seller-1",
                "seller-1@example.com",
                "0900000000",
                UserRole.ROLE_SELLER,
                UserTier.VIP
        ));
        userService.delete(created.id());

        assertThat(updated.username()).isEqualTo("seller-1");
        assertThat(updated.tier()).isEqualTo(UserTier.VIP);
        assertThatThrownBy(() -> userService.findById(created.id()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void searchesWithPaginationAndFilters() {
        userService.create(new CreateUserRequest("alice", "alice@example.com", "0901", UserRole.ROLE_CUSTOMER, UserTier.STANDARD));
        userService.create(new CreateUserRequest("bob", "bob@example.com", "0902", UserRole.ROLE_VIP, UserTier.VIP));
        userService.create(new CreateUserRequest("carol", "carol@example.com", "0903", UserRole.ROLE_CUSTOMER, UserTier.VIP));

        PageResponse<UserResponse> result = userService.search(new UserSearchRequest(
                "example.com",
                UserRole.ROLE_CUSTOMER,
                UserTier.VIP,
                0,
                10
        ));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.content()).extracting(UserResponse::username)
                .containsExactly("carol");
    }

    @Test
    void rejectsDuplicateUsernameOrEmail() {
        userService.create(new CreateUserRequest("alice", "alice@example.com", null, UserRole.ROLE_CUSTOMER, UserTier.STANDARD));

        assertThatThrownBy(() -> userService.create(new CreateUserRequest(
                "alice",
                "alice2@example.com",
                null,
                UserRole.ROLE_CUSTOMER,
                UserTier.STANDARD
        ))).isInstanceOf(BusinessException.class);
    }
}

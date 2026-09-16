package com.nchuy099.ecommerce.user.dto;

import com.nchuy099.ecommerce.user.entity.UserRole;
import com.nchuy099.ecommerce.user.entity.UserTier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Email @Size(max = 160) String email,
        @Size(max = 32) @Pattern(regexp = "^[0-9+() .-]*$", message = "must be a phone-like value") String phone,
        @NotNull UserRole role,
        @NotNull UserTier tier
) {
}

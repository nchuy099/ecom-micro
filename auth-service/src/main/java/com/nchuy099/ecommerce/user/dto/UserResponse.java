package com.nchuy099.ecommerce.user.dto;

import java.time.Instant;

import com.nchuy099.ecommerce.user.entity.UserEntity;
import com.nchuy099.ecommerce.user.entity.UserRole;
import com.nchuy099.ecommerce.user.entity.UserTier;

public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        UserRole role,
        UserTier tier,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getTier(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

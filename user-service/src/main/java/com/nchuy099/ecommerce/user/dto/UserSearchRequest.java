package com.nchuy099.ecommerce.user.dto;

import com.nchuy099.ecommerce.user.entity.UserRole;
import com.nchuy099.ecommerce.user.entity.UserTier;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UserSearchRequest(
        @Size(max = 160) String keyword,
        UserRole role,
        UserTier tier,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? 20 : size;
    }
}

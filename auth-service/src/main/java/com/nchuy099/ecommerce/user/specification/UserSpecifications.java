package com.nchuy099.ecommerce.user.specification;

import com.nchuy099.ecommerce.user.entity.UserEntity;
import com.nchuy099.ecommerce.user.entity.UserRole;
import com.nchuy099.ecommerce.user.entity.UserTier;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<UserEntity> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            String rawPattern = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(root.get("phone"), rawPattern)
            );
        };
    }

    public static Specification<UserEntity> hasRole(UserRole role) {
        return (root, query, cb) -> role == null ? cb.conjunction() : cb.equal(root.get("role"), role);
    }

    public static Specification<UserEntity> hasTier(UserTier tier) {
        return (root, query, cb) -> tier == null ? cb.conjunction() : cb.equal(root.get("tier"), tier);
    }
}

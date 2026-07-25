package com.nchuy099.ecommerce.product.specification;

import java.math.BigDecimal;

import com.nchuy099.ecommerce.product.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {
    private ProductSpecifications() {
    }

    public static Specification<ProductEntity> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("sku")), pattern)
            );
        };
    }

    public static Specification<ProductEntity> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? cb.conjunction() : cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<ProductEntity> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<ProductEntity> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}

package com.nchuy099.ecommerce.order.pagination;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.nchuy099.ecommerce.order.exception.BusinessException;

public record OrderCursor(Instant createdAt, Long id) {
    public static String encode(Instant createdAt, Long id) {
        String value = createdAt.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static OrderCursor decode(String cursor) {
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = value.split("\\|", -1);
            if (parts.length != 2) {
                throw invalidCursor();
            }
            return new OrderCursor(Instant.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "https://errors.ecom.local/invalid-pagination",
                    "Invalid pagination request",
                    "Invalid order cursor",
                    ex
            );
        }
    }

    private static BusinessException invalidCursor() {
        return BusinessException.badRequest(
                "https://errors.ecom.local/invalid-pagination",
                "Invalid pagination request",
                "Invalid order cursor"
        );
    }
}

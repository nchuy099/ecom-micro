package com.nchuy099.ecommerce.product.search;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.nchuy099.ecommerce.product.exception.BusinessException;

public final class ProductCursorCodec {
    private ProductCursorCodec() {
    }

    public static String encode(Long id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
    }

    public static long decode(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            long id = Long.parseLong(decoded);
            if (id < 0) {
                throw BusinessException.badRequest(
                        "https://errors.ecom.local/invalid-pagination",
                        "Invalid pagination request",
                        "Cursor must contain a positive product id"
                );
            }
            return id;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "https://errors.ecom.local/invalid-pagination",
                    "Invalid pagination request",
                    "Invalid product cursor",
                    ex
            );
        }
    }
}

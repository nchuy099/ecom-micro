package com.nchuy099.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nchuy099.ecommerce.product.search.ProductCursorCodec;
import com.nchuy099.ecommerce.product.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ProductCursorCodecTest {
    @Test
    void encodesAndDecodesProductId() {
        String cursor = ProductCursorCodec.encode(42L);

        assertThat(ProductCursorCodec.decode(cursor)).isEqualTo(42L);
        assertThat(cursor).doesNotContain("42");
    }

    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> ProductCursorCodec.decode("invalid"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid product cursor");
    }
}

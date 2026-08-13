package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import com.nchuy099.ecommerce.order.client.ProductClient;
import com.nchuy099.ecommerce.order.client.dto.ProductClientResponse;
import com.nchuy099.ecommerce.order.exception.ProductReservationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProductClientTest {
    private MockRestServiceServer mockServer;
    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8082");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        productClient = new ProductClient(restClient);
    }

    @Test
    void reserveStockSuccess() {
        mockServer.expect(requestTo("http://localhost:8082/v1/products/1/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "id": 1,
                            "name": "Phone",
                            "sku": "PH-1",
                            "price": 500.00,
                            "stock": 5,
                            "categoryId": 1,
                            "version": 1
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ProductClientResponse response = productClient.reserveStock(1L, 2);
        mockServer.verify();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Phone");
        assertThat(response.stock()).isEqualTo(5);
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void reserveStockInsufficientStockPropagatesConflict() {
        mockServer.expect(requestTo("http://localhost:8082/v1/products/1/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body("""
                        {
                          "type": "https://errors.ecom.local/insufficient-stock",
                          "title": "Insufficient stock",
                          "status": 409,
                          "detail": "Insufficient stock for product 1"
                        }
                        """));

        assertThatThrownBy(() -> productClient.reserveStock(1L, 10))
                .isInstanceOf(ProductReservationException.class)
                .satisfies(ex -> assertThat(((ProductReservationException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        mockServer.verify();
    }

    @Test
    void releaseStockSuccess() {
        mockServer.expect(requestTo("http://localhost:8082/v1/products/1/release"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "id": 1,
                            "name": "Phone",
                            "sku": "PH-1",
                            "price": 500.00,
                            "stock": 7,
                            "categoryId": 1,
                            "version": 2
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ProductClientResponse response = productClient.releaseStock(1L, 2);
        mockServer.verify();

        assertThat(response.stock()).isEqualTo(7);
    }
}

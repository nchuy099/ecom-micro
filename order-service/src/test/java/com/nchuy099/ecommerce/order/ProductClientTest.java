package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nchuy099.ecommerce.order.client.ProductClient;
import com.nchuy099.ecommerce.order.client.dto.ProductClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
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
    void reserveStockForOrderUsesSynchronousReservationEndpoint() {
        mockServer.expect(requestTo("http://localhost:8082/v1/products/1/reserve-for-order"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "id": 1,
                            "name": "Phone",
                            "sku": "PH-1",
                            "price": 500.00,
                            "stock": 3,
                            "categoryId": 1,
                            "version": 2
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ProductClientResponse response = productClient.reserveStockForOrder(99L, 100L, 1L, 2);

        mockServer.verify();
        assertThat(response.stock()).isEqualTo(3);
    }

    @Test
    void releaseOrderStockUsesCompensationEndpoint() {
        mockServer.expect(requestTo("http://localhost:8082/v1/products/reservations/99/release"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        productClient.releaseOrderStock(99L);

        mockServer.verify();
    }
}

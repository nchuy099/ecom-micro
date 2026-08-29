package com.nchuy099.ecommerce.order;

import com.nchuy099.ecommerce.order.client.ProductClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class ProductClientTimeoutConfigTest {

    @Test
    void verifiesConnectAndReadTimeoutsAreConfigured() {
        ProductClientConfig config = new ProductClientConfig();
        RestClient.Builder builder = RestClient.builder();

        RestClient restClient = config.productRestClient(
                builder,
                "http://localhost:8082",
                3000,
                5000
        );

        assertThat(restClient).isNotNull();

        ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory) ReflectionTestUtils.getField(builder, "requestFactory");
        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);

        SimpleClientHttpRequestFactory simpleFactory = (SimpleClientHttpRequestFactory) requestFactory;
        Integer connectTimeout = (Integer) ReflectionTestUtils.getField(simpleFactory, "connectTimeout");
        Integer readTimeout = (Integer) ReflectionTestUtils.getField(simpleFactory, "readTimeout");

        assertThat(connectTimeout).isEqualTo(3000);
        assertThat(readTimeout).isEqualTo(5000);
    }
}

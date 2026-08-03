package com.nchuy099.ecommerce.order.client;

import com.nchuy099.ecommerce.common.ApiResponse;
import com.nchuy099.ecommerce.order.client.dto.ProductClientResponse;
import com.nchuy099.ecommerce.order.client.dto.StockQuantityRequest;
import com.nchuy099.ecommerce.order.exception.ProductReservationException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {
    private final RestClient restClient;

    public ProductClient(RestClient productRestClient) {
        this.restClient = productRestClient;
    }

    @CircuitBreaker(name = "productService")
    @Retry(name = "productService")
    @Bulkhead(name = "productService")
    public ProductClientResponse reserveStock(Long productId, Integer quantity) {
        try {
            ApiResponse<ProductClientResponse> response = restClient.post()
                    .uri("/v1/products/{id}/reserve", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockQuantityRequest(quantity))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<ProductClientResponse>>() {});

            if (response == null || response.data() == null) {
                throw new ProductReservationException("Empty response received from product service", HttpStatus.BAD_GATEWAY);
            }
            return response.data();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ProductReservationException(
                    "Product reservation failed for product " + productId + ": " + ex.getResponseBodyAsString(),
                    ex,
                    ex.getStatusCode()
            );
        } catch (ResourceAccessException ex) {
            throw new ProductReservationException(
                    "Product service connection timed out or unreachable for product " + productId + ": " + ex.getMessage(),
                    ex,
                    HttpStatus.GATEWAY_TIMEOUT
            );
        } catch (ProductReservationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProductReservationException(
                    "Unexpected error during product reservation for product " + productId + ": " + ex.getMessage(),
                    ex,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public ProductClientResponse releaseStock(Long productId, Integer quantity) {
        try {
            ApiResponse<ProductClientResponse> response = restClient.post()
                    .uri("/v1/products/{id}/release", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockQuantityRequest(quantity))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<ProductClientResponse>>() {});

            return response != null ? response.data() : null;
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ProductReservationException(
                    "Product release failed for product " + productId + ": " + ex.getResponseBodyAsString(),
                    ex,
                    ex.getStatusCode()
            );
        } catch (ResourceAccessException ex) {
            throw new ProductReservationException(
                    "Product service connection timed out or unreachable for product release " + productId,
                    ex,
                    HttpStatus.GATEWAY_TIMEOUT
            );
        } catch (Exception ex) {
            throw new ProductReservationException(
                    "Unexpected error during product release for product " + productId + ": " + ex.getMessage(),
                    ex,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}

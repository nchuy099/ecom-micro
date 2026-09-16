package com.nchuy099.ecommerce.order.client;

import com.nchuy099.ecommerce.order.api.ApiResponse;
import com.nchuy099.ecommerce.order.client.dto.ProductClientResponse;
import com.nchuy099.ecommerce.order.client.dto.OrderStockReservationRequest;
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
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductClient {
    private final RestClient restClient;

    @CircuitBreaker(name = "productService")
    @Retry(name = "productService")
    @Bulkhead(name = "productService")
    public ProductClientResponse reserveStockForOrder(Long orderId, Long userId, Long productId, Integer quantity) {
        try {
            ApiResponse<ProductClientResponse> response = restClient.post()
                    .uri("/v1/products/{id}/reserve-for-order", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OrderStockReservationRequest(orderId, userId, quantity))
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

    @Retry(name = "productService")
    @Bulkhead(name = "productService")
    public void releaseOrderStock(Long orderId) {
        try {
            restClient.post()
                    .uri("/v1/products/reservations/{orderId}/release", orderId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ProductReservationException(
                    "Product reservation release failed for order " + orderId + ": " + ex.getResponseBodyAsString(),
                    ex,
                    ex.getStatusCode()
            );
        } catch (ResourceAccessException ex) {
            throw new ProductReservationException(
                    "Product service connection timed out or unreachable while releasing order " + orderId,
                    ex,
                    HttpStatus.GATEWAY_TIMEOUT
            );
        } catch (Exception ex) {
            throw new ProductReservationException(
                    "Unexpected error while releasing product reservation for order " + orderId + ": " + ex.getMessage(),
                    ex,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

}

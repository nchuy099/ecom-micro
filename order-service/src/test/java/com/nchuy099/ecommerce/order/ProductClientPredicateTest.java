package com.nchuy099.ecommerce.order;

import com.nchuy099.ecommerce.order.client.resilience.ProductClientRecordFailurePredicate;
import com.nchuy099.ecommerce.order.client.resilience.ProductClientRetryPredicate;
import com.nchuy099.ecommerce.order.exception.ProductReservationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class ProductClientPredicateTest {

    private final ProductClientRetryPredicate retryPredicate = new ProductClientRetryPredicate();
    private final ProductClientRecordFailurePredicate failurePredicate = new ProductClientRecordFailurePredicate();

    @Test
    void conflict409IsNotRetriedAndNotRecordedAsFailure() {
        ProductReservationException ex = new ProductReservationException("Insufficient stock", HttpStatus.CONFLICT);

        assertThat(retryPredicate.test(ex)).isFalse();
        assertThat(failurePredicate.test(ex)).isFalse();
    }

    @Test
    void badRequest400IsNotRetriedAndNotRecordedAsFailure() {
        ProductReservationException ex = new ProductReservationException("Bad request", HttpStatus.BAD_REQUEST);

        assertThat(retryPredicate.test(ex)).isFalse();
        assertThat(failurePredicate.test(ex)).isFalse();
    }

    @Test
    void serverError503IsRetriedAndRecordedAsFailure() {
        ProductReservationException ex = new ProductReservationException("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(retryPredicate.test(ex)).isTrue();
        assertThat(failurePredicate.test(ex)).isTrue();
    }

    @Test
    void serverError500IsRetriedAndRecordedAsFailure() {
        ProductReservationException ex = new ProductReservationException("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(retryPredicate.test(ex)).isTrue();
        assertThat(failurePredicate.test(ex)).isTrue();
    }

    @Test
    void resourceAccessExceptionIsRetriedAndRecordedAsFailure() {
        ResourceAccessException timeoutEx = new ResourceAccessException("Connection timed out");
        ProductReservationException wrapped = new ProductReservationException("Timed out", timeoutEx, HttpStatus.GATEWAY_TIMEOUT);

        assertThat(retryPredicate.test(wrapped)).isTrue();
        assertThat(failurePredicate.test(wrapped)).isTrue();
        assertThat(retryPredicate.test(timeoutEx)).isTrue();
        assertThat(failurePredicate.test(timeoutEx)).isTrue();
    }
}

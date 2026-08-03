package com.nchuy099.ecommerce.order.client.resilience;

import java.util.function.Predicate;

import com.nchuy099.ecommerce.order.exception.ProductReservationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;

public class ProductClientRecordFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof ProductReservationException pre) {
            HttpStatusCode status = pre.getStatusCode();
            if (status != null && status.is4xxClientError()) {
                // 4xx business conflicts (e.g. 409 Conflict) do NOT count as circuit breaker failures
                return false;
            }
            return true; // 5xx, timeouts, connection errors count as failures
        }
        return throwable instanceof ResourceAccessException;
    }
}

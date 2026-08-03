package com.nchuy099.ecommerce.order.client.resilience;

import java.util.function.Predicate;

import com.nchuy099.ecommerce.order.exception.ProductReservationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;

public class ProductClientRetryPredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof ProductReservationException pre) {
            HttpStatusCode status = pre.getStatusCode();
            if (status != null) {
                if (status.is4xxClientError()) {
                    // Do NOT retry 4xx errors (e.g. 409 Conflict, 400 Bad Request)
                    return false;
                }
                if (status.is5xxServerError()) {
                    // Retry transient 5xx server errors
                    return true;
                }
            }
            if (pre.getCause() instanceof ResourceAccessException) {
                return true;
            }
        }
        return throwable instanceof ResourceAccessException;
    }
}

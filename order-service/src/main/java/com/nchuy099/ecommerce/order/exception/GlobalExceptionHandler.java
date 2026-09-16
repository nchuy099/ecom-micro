package com.nchuy099.ecommerce.order.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setType(ex.getType());
        problem.setTitle(ex.getTitle());
        ex.getProperties().forEach(problem::setProperty);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatus());
        Object retryAfterSeconds = ex.getProperties().get("retryAfterSeconds");
        if (retryAfterSeconds instanceof Number retryAfter) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter.longValue()));
        }
        return response.body(problem);
    }

    @ExceptionHandler(ProductReservationException.class)
    ProblemDetail handleProductReservation(ProductReservationException ex) {
        HttpStatusCode status = ex.getStatusCode() != null ? ex.getStatusCode() : HttpStatus.BAD_GATEWAY;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(URI.create("https://errors.ecom.local/product-reservation-failed"));
        problem.setTitle("Product reservation failed");
        return problem;
    }

    @ExceptionHandler(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class)
    ProblemDetail handleCallNotPermitted(io.github.resilience4j.circuitbreaker.CallNotPermittedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Product service is temporarily unavailable (circuit breaker is open)"
        );
        problem.setType(URI.create("https://errors.ecom.local/product-service-unavailable"));
        problem.setTitle("Service Unavailable");
        return problem;
    }

    @ExceptionHandler(io.github.resilience4j.bulkhead.BulkheadFullException.class)
    ProblemDetail handleBulkheadFull(io.github.resilience4j.bulkhead.BulkheadFullException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many concurrent requests to product service"
        );
        problem.setType(URI.create("https://errors.ecom.local/too-many-requests"));
        problem.setTitle("Too Many Requests");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setType(URI.create("https://errors.ecom.local/validation-failed"));
        problem.setTitle("Validation failed");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformed(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
        problem.setType(URI.create("https://errors.ecom.local/malformed-request"));
        problem.setTitle("Malformed request");
        return problem;
    }

}

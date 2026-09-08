package com.nchuy099.ecommerce.notification.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotificationDlqNotFoundException.class)
    ProblemDetail handleDlqNotFound(NotificationDlqNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://errors.ecom.local/notification-dlq-not-found"));
        problem.setTitle("Notification DLQ entry not found");
        return problem;
    }
}

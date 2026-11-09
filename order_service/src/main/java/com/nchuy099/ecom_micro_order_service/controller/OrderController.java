package com.nchuy099.ecom_micro_order_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.idempotency.OrderIdempotencyService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderIdempotencyService idempotency;

    @PostMapping
    public ResponseEntity<String> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody @Valid CreateOrderReq req,
            @AuthenticationPrincipal Jwt jwt)
            throws JsonProcessingException {
        if (key == null) return ResponseEntity.badRequest().build();
        try {
            if (!UUID.fromString(key).toString().equalsIgnoreCase(key))
                return ResponseEntity.badRequest().build();
            key = UUID.fromString(key).toString();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
        String customer = jwt.getClaimAsString("customer_id");
        if (customer == null || !customer.equals(req.getCustomerId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (jwt.getSubject() == null || jwt.getSubject().isBlank())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            var result = idempotency.create(jwt.getSubject(), key, req);
            return ResponseEntity.status(result.status())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.body());
        } catch (OrderIdempotencyService.IdempotencyConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (PessimisticLockingFailureException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).header("Retry-After", "1").build();
        }
    }
}

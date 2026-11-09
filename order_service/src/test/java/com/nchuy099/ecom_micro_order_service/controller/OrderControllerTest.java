package com.nchuy099.ecom_micro_order_service.controller;

import com.nchuy099.ecom_micro_order_service.dto.request.CreateOrderReq;
import com.nchuy099.ecom_micro_order_service.idempotency.OrderIdempotencyService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderControllerTest {
    @Test
    void checksKeyAndCustomerClaimAndReplaysResponse() throws Exception {
        var service = mock(OrderIdempotencyService.class);
        var controller = new OrderController(service);
        var req = new CreateOrderReq();
        req.setCustomerId("customer");
        var jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("user")
                        .claim("customer_id", "customer")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build();
        assertEquals(HttpStatus.BAD_REQUEST, controller.create(null, req, jwt).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST, controller.create("not-uuid", req, jwt).getStatusCode());
        req.setCustomerId("another");
        assertEquals(
                HttpStatus.FORBIDDEN,
                controller
                        .create("c1290b50-14c8-4c99-8a47-ce3b48971459", req, jwt)
                        .getStatusCode());
        req.setCustomerId("customer");
        when(service.create(anyString(), anyString(), any()))
                .thenReturn(new OrderIdempotencyService.Result(200, "{\"data\":{}}"));
        var response = controller.create("c1290b50-14c8-4c99-8a47-ce3b48971459", req, jwt);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"data\":{}}", response.getBody());
        verify(service).create(eq("user"), anyString(), same(req));
    }
}

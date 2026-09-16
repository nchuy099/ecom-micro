package com.nchuy099.ecommerce.order;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nchuy099.ecommerce.order.api.ApiResponse;
import com.nchuy099.ecommerce.order.controller.OrderController;
import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderItemResponse;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.dto.PageResponse;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.exception.ProductReservationException;
import com.nchuy099.ecommerce.order.idempotency.OrderIdempotencyService;
import com.nchuy099.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderIdempotencyService orderIdempotencyService;

    @Test
    void createsOrder() throws Exception {
        OrderResponse response = orderResponse(1L, "ORD-12345", 100L, OrderStatus.CONFIRMED);
        when(orderService.create(any())).thenReturn(response);

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                100L,
                                List.of(new OrderItemRequest(10L, 2))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-12345"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void createsOrderWithIdempotencyKey() throws Exception {
        OrderResponse response = orderResponse(1L, "ORD-12345", 100L, OrderStatus.CONFIRMED);
        when(orderIdempotencyService.executeCreateOrder(eq("100"), eq("idem-123"), any())).thenReturn(ApiResponse.of(response));

        mockMvc.perform(post("/v1/orders")
                        .header("Idempotency-Key", "idem-123")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                100L,
                                List.of(new OrderItemRequest(10L, 2))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-12345"));
    }

    @Test
    void readsOrderAndListsByUser() throws Exception {
        when(orderService.findById(1L)).thenReturn(orderResponse(1L, "ORD-12345", 100L, OrderStatus.CONFIRMED));
        when(orderService.findByUserId(eq(100L), any())).thenReturn(
                PageResponse.of(List.of(orderResponse(1L, "ORD-12345", 100L, OrderStatus.CONFIRMED)), 0, 10, 1)
        );

        mockMvc.perform(get("/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-12345"));

        mockMvc.perform(get("/v1/orders?userId=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-12345"));

        mockMvc.perform(get("/v1/orders/user/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-12345"));
    }

    @Test
    void cancelsOrder() throws Exception {
        when(orderService.cancel(1L)).thenReturn(orderResponse(1L, "ORD-12345", 100L, OrderStatus.CANCELLED));

        mockMvc.perform(post("/v1/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void returnsProblemDetailForValidationFailure() throws Exception {
        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": null,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void returnsProblemDetailForDomainErrors() throws Exception {
        when(orderService.findById(99L)).thenThrow(BusinessException.notFound(
                "https://errors.ecom.local/order-not-found", "Order not found", "Order not found with id: 99"));
        when(orderService.cancel(99L)).thenThrow(BusinessException.conflict(
                "https://errors.ecom.local/order-conflict", "Order state conflict", "Cannot cancel order with status CANCELLED"));
        when(orderService.create(any())).thenThrow(new ProductReservationException("Insufficient stock", HttpStatus.CONFLICT));

        mockMvc.perform(get("/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"));

        mockMvc.perform(post("/v1/orders/99/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Order state conflict"));

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                100L,
                                List.of(new OrderItemRequest(10L, 2))
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Product reservation failed"));
    }

    private static OrderResponse orderResponse(Long id, String orderNumber, Long userId, OrderStatus status) {
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        return new OrderResponse(
                id,
                orderNumber,
                userId,
                status,
                new BigDecimal("2400.00"),
                List.of(new OrderItemResponse(1L, 10L, "Laptop", 2, new BigDecimal("1200.00"), new BigDecimal("2400.00"))),
                now,
                now
        );
    }
}

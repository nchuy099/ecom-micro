package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.client.ProductClient;
import com.nchuy099.ecommerce.order.client.dto.ProductClientResponse;
import com.nchuy099.ecommerce.order.entity.OrderEntity;
import com.nchuy099.ecommerce.order.entity.OrderItemEntity;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.BusinessException;
import com.nchuy099.ecommerce.order.repository.OrderRepository;
import com.nchuy099.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productClient);
    }

    @Test
    void createsConfirmedOrderAfterSynchronousStockReservation() {
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            setId(order, 10L);
            return order;
        });
        when(productClient.reserveStockForOrder(10L, 100L, 1L, 2))
                .thenReturn(new ProductClientResponse(1L, "Laptop", "LAP-1", new BigDecimal("500.00"), 8, null, 1L));

        OrderResponse response = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(1L, 2))
        ));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.items().get(0).productName()).isEqualTo("Laptop");
    }

    @Test
    void propagatesDatabaseFailureBeforeReservation() {
        when(orderRepository.saveAndFlush(any())).thenThrow(new RuntimeException("Database connection failure"));

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 1))
        )))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failure");

    }

    @Test
    void compensatesEarlierReservationsWhenAnotherProductFails() {
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            setId(order, 10L);
            return order;
        });
        when(productClient.reserveStockForOrder(10L, 100L, 1L, 2))
                .thenReturn(new ProductClientResponse(1L, "Laptop", "LAP-1", new BigDecimal("500.00"), 8, null, 1L));
        when(productClient.reserveStockForOrder(10L, 100L, 2L, 1))
                .thenThrow(new RuntimeException("Insufficient stock"));

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 1))
        ))).isInstanceOf(RuntimeException.class);

        verify(productClient).releaseOrderStock(10L);
    }

    @Test
    void cancelsConfirmedOrderAfterSynchronousReservationRelease() {
        OrderEntity order = new OrderEntity("ORD-001", 100L, OrderStatus.CONFIRMED, new BigDecimal("1000.00"));
        OrderItemEntity item = new OrderItemEntity(order, 1L, "Laptop", 2, new BigDecimal("500.00"), new BigDecimal("1000.00"));
        order.addItem(item);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse cancelled = orderService.cancel(1L);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);

        verify(productClient).releaseOrderStock(1L);
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void cancelRejectsAlreadyCancelledOrderWithoutCallingProductClient() {
        OrderEntity order = new OrderEntity("ORD-001", 100L, OrderStatus.CANCELLED, new BigDecimal("1000.00"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel order with status CANCELLED");

        verify(orderRepository, never()).saveAndFlush(any());
        verify(productClient, never()).releaseOrderStock(any());
    }

    private static void setId(OrderEntity order, Long id) {
        try {
            java.lang.reflect.Field field = OrderEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

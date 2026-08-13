package com.nchuy099.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.nchuy099.ecommerce.common.event.PaymentFailedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedEvent;
import com.nchuy099.ecommerce.common.event.StockReservedItem;
import com.nchuy099.ecommerce.order.dto.CreateOrderRequest;
import com.nchuy099.ecommerce.order.dto.OrderItemRequest;
import com.nchuy099.ecommerce.order.dto.OrderResponse;
import com.nchuy099.ecommerce.order.entity.OrderEntity;
import com.nchuy099.ecommerce.order.entity.OrderItemEntity;
import com.nchuy099.ecommerce.order.entity.OrderStatus;
import com.nchuy099.ecommerce.order.exception.OrderStateConflictException;
import com.nchuy099.ecommerce.order.repository.OrderRepository;
import com.nchuy099.ecommerce.order.service.OutboxEventService;
import com.nchuy099.ecommerce.order.service.OrderService;
import com.nchuy099.ecommerce.order.service.ProcessedEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private ProcessedEventService processedEventService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, outboxEventService, processedEventService);
    }

    @Test
    void createsPendingOrderAndWritesOutboxEvent() {
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            setId(order, 10L);
            return order;
        });

        OrderResponse response = orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(1L, 2))
        ));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(outboxEventService, times(1)).writeOrderCreated(any());
    }

    @Test
    void doesNotWriteOutboxWhenDatabaseSaveFails() {
        when(orderRepository.saveAndFlush(any())).thenThrow(new RuntimeException("Database connection failure"));

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(
                100L,
                List.of(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 1))
        )))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failure");

        verify(outboxEventService, never()).writeOrderCreated(any());
    }

    @Test
    void cancelsConfirmedOrderAndWritesOutboxEvent() {
        OrderEntity order = new OrderEntity("ORD-001", 100L, OrderStatus.CONFIRMED, new BigDecimal("1000.00"));
        OrderItemEntity item = new OrderItemEntity(order, 1L, "Laptop", 2, new BigDecimal("500.00"), new BigDecimal("1000.00"));
        order.addItem(item);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse cancelled = orderService.cancel(1L);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);

        InOrder inOrder = inOrder(orderRepository, outboxEventService);
        inOrder.verify(orderRepository).saveAndFlush(order);
        inOrder.verify(outboxEventService).writeOrderCancelled(any());
    }

    @Test
    void cancelRejectsAlreadyCancelledOrderWithoutCallingProductClient() {
        OrderEntity order = new OrderEntity("ORD-001", 100L, OrderStatus.CANCELLED, new BigDecimal("1000.00"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(1L))
                .isInstanceOf(OrderStateConflictException.class)
                .hasMessageContaining("Cannot cancel order with status CANCELLED");

        verify(orderRepository, never()).saveAndFlush(any());
        verify(outboxEventService, never()).writeOrderCancelled(any());
    }

    @Test
    void stockReservedUpdatesPendingOrderTotals() {
        OrderEntity order = new OrderEntity("ORD-001", 100L, OrderStatus.PENDING, BigDecimal.ZERO);
        order.addItem(OrderItemEntity.pending(order, 1L, 2));
        setId(order, 1L);
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(processedEventService.processOnce(any(), any(), any())).thenAnswer(invocation -> {
            Supplier<Boolean> handler = invocation.getArgument(2);
            return handler.get();
        });

        orderService.applyStockReserved(StockReservedEvent.of(
                1L,
                100L,
                List.of(new StockReservedItem(1L, "Laptop", 2, new BigDecimal("500.00"), new BigDecimal("1000.00")))
        ));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(order.findItem(1L).getProductName()).isEqualTo("Laptop");
    }

    @Test
    void paymentFailedCancelsOrderAndWritesOrderCancelledEvent() {
        OrderEntity order = new OrderEntity("ORD-001", 100L, OrderStatus.PENDING, BigDecimal.ZERO);
        setId(order, 1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(processedEventService.processOnce(any(), any(), any())).thenAnswer(invocation -> {
            Supplier<Boolean> handler = invocation.getArgument(2);
            return handler.get();
        });

        orderService.applyPaymentFailed(PaymentFailedEvent.of(1L, 100L, "Mock payment failed"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(outboxEventService).writeOrderCancelled(any());
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

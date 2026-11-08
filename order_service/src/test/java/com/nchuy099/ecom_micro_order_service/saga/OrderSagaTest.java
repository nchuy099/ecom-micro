package com.nchuy099.ecom_micro_order_service.saga;

import com.nchuy099.ecom_micro_order_service.clients.ProductClient;
import com.nchuy099.ecom_micro_order_service.entity.OrderEntity;
import com.nchuy099.ecom_micro_order_service.repository.*;
import com.nchuy099.ecom_micro_order_service.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderSagaTest {
    @Test
    void cancelPublishesOnceAndLateConfirmationIsIgnored() {
        var orders = mock(OrderRepository.class);
        var items = mock(OrderItemRepository.class);
        var outbox = mock(SagaOutbox.class);
        var order = OrderEntity.builder().id("order-1").status("PENDING_PAYMENT").build();
        when(orders.findForUpdate("order-1")).thenReturn(Optional.of(order));
        when(items.findByOrderId("order-1")).thenReturn(List.of());
        var service = new OrderServiceImpl(orders, items, mock(ProductClient.class), outbox);
        service.cancel("order-1", "declined");
        service.cancel("order-1", "declined");
        service.confirm("order-1");
        assertEquals("CANCELLED", order.getStatus());
        verify(outbox, times(1)).add(eq("order-cancelled"), eq("order-1"), any());
        verify(outbox, never()).add(eq("order-confirmed"), anyString(), any());
    }

    @Test
    void paymentCanArriveBeforeStockNotification() {
        var orders = mock(OrderRepository.class);
        var outbox = mock(SagaOutbox.class);
        var order = OrderEntity.builder().id("order-1").status("PENDING_STOCK").build();
        when(orders.findForUpdate("order-1")).thenReturn(Optional.of(order));
        var service =
                new OrderServiceImpl(
                        orders, mock(OrderItemRepository.class), mock(ProductClient.class), outbox);
        service.confirm("order-1");
        service.updateOrderStatusLocked("order-1");
        assertEquals("CONFIRMED", order.getStatus());
        verify(outbox).add(eq("order-confirmed"), eq("order-1"), any());
    }
}

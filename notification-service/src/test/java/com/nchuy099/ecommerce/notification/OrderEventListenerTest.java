package com.nchuy099.ecommerce.notification;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nchuy099.ecommerce.common.event.OrderCreatedEvent;
import com.nchuy099.ecommerce.notification.listener.OrderEventListener;
import com.nchuy099.ecommerce.notification.service.NotificationFanOutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {
    @Mock
    private NotificationFanOutService fanOutService;

    @Test
    void convertsOrderCreatedToNotificationRequest() {
        OrderEventListener listener = new OrderEventListener(fanOutService);
        OrderCreatedEvent event = OrderCreatedEvent.of(500L, 600L);

        listener.handleOrderCreated(event);

        verify(fanOutService).fanOut(argThat(notification ->
                notification.campaignId().equals("order-500")
                        && notification.userId().equals(600L)
                        && notification.subject().equals("Order 500 created")
        ));
    }

    @Test
    void ignoresNullEvent() {
        OrderEventListener listener = new OrderEventListener(fanOutService);

        listener.handleOrderCreated(null);

        verify(fanOutService, never()).fanOut(org.mockito.ArgumentMatchers.any());
    }
}

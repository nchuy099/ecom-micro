package com.nchuy099.ecommerce.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import com.nchuy099.ecommerce.notification.event.OrderConfirmedEvent;
import com.nchuy099.ecommerce.notification.publisher.NotificationTaskPublisher;
import com.nchuy099.ecommerce.notification.listener.OrderEventListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {
    @Mock
    private NotificationTaskPublisher publisher;

    @Test
    void convertsOneOrderEventIntoTasksForAllChannels() {
        new OrderEventListener(publisher).handleOrderConfirmed(new OrderConfirmedEvent(
                UUID.randomUUID(), "ORDER_CONFIRMED", 7L, 42L, "ORD-7", "Confirmed", Instant.now()
        ));

        verify(publisher, times(3)).publish(any());
    }
}

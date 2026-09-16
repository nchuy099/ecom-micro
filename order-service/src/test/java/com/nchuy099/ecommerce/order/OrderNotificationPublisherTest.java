package com.nchuy099.ecommerce.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nchuy099.ecommerce.order.event.KafkaTopics;
import com.nchuy099.ecommerce.order.event.NotificationRequestedEvent;
import com.nchuy099.ecommerce.order.notification.OrderNotificationPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class OrderNotificationPublisherTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void publishesConfirmedOrderAsNotificationRequest() {
        OrderNotificationPublisher publisher = new OrderNotificationPublisher(kafkaTemplate, true);

        publisher.publishOrderConfirmed(42L, 100L, "ORD-42", "Order confirmed");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.NOTIFICATION_REQUESTED), eq("42"), eventCaptor.capture());
        NotificationRequestedEvent event = (NotificationRequestedEvent) eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(event.campaignId()).isEqualTo("order:42");
        org.assertj.core.api.Assertions.assertThat(event.userId()).isEqualTo(100L);
        org.assertj.core.api.Assertions.assertThat(event.subject()).isEqualTo("Order confirmed: ORD-42");
    }

    @Test
    void canBeDisabledForLocalTestsOrDeploymentsWithoutKafka() {
        OrderNotificationPublisher publisher = new OrderNotificationPublisher(kafkaTemplate, false);

        publisher.publishOrderConfirmed(42L, 100L, "ORD-42", "Order confirmed");

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}

package com.nchuy099.ecom_micro_order_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class SagaTopicsConfig {
    @Bean
    NewTopic orderCreated() {
        return topic("order-created");
    }

    @Bean
    NewTopic stockReserved() {
        return topic("stock-reserved");
    }

    @Bean
    NewTopic stockReservationFailed() {
        return topic("stock-reservation-failed");
    }

    @Bean
    NewTopic paymentCompleted() {
        return topic("payment-completed");
    }

    @Bean
    NewTopic paymentFailed() {
        return topic("payment-failed");
    }

    @Bean
    NewTopic stockReleased() {
        return topic("stock-released");
    }

    @Bean
    NewTopic orderConfirmed() {
        return topic("order-confirmed");
    }

    @Bean
    NewTopic orderCancelled() {
        return topic("order-cancelled");
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }
}

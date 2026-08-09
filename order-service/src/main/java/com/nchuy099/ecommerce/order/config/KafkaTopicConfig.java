package com.nchuy099.ecommerce.order.config;

import com.nchuy099.ecommerce.common.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "ecommerce.kafka.topics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CANCELLED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockReservedTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK_RESERVED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stockReservationFailedTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK_RESERVATION_FAILED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stockReleasedTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK_RELEASED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_COMPLETED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_FAILED).partitions(1).replicas(1).build();
    }
}

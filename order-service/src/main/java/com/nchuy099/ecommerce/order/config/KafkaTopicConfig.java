package com.nchuy099.ecommerce.order.config;

import com.nchuy099.ecommerce.order.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "ecommerce.kafka.topics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {
    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic notificationTasksTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_TASKS).partitions(12).replicas(1).build();
    }
}

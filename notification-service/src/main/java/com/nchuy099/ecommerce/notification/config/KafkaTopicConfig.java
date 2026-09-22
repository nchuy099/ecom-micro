package com.nchuy099.ecommerce.notification.config;

import com.nchuy099.ecommerce.notification.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "ecommerce.kafka.topics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {
    @Bean
    NewTopic notificationRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_REQUESTED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic notificationTasksTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_TASKS).partitions(12).replicas(1).build();
    }

    @Bean
    NewTopic notificationRetryTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_RETRY).partitions(12).replicas(1).build();
    }

    @Bean
    NewTopic notificationDlqTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_DLQ).partitions(12).replicas(1).build();
    }

    @Bean
    NewTopic notificationEmailRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_EMAIL_REQUESTED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic notificationPushRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_PUSH_REQUESTED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic notificationSmsRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_SMS_REQUESTED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic notificationEmailDlqTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_EMAIL_DLQ).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic notificationPushDlqTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_PUSH_DLQ).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic notificationSmsDlqTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_SMS_DLQ).partitions(1).replicas(1).build();
    }
}

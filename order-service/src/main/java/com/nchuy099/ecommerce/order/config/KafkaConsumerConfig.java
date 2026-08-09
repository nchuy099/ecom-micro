package com.nchuy099.ecommerce.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

@Configuration
public class KafkaConsumerConfig {
    @Bean
    RecordMessageConverter kafkaRecordMessageConverter() {
        return new StringJsonMessageConverter();
    }
}

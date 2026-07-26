package com.nchuy099.ecommerce.product.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nchuy099.ecommerce.product.dto.ProductResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductCacheConfig {

    @Bean
    public Cache<Long, ProductResponse> productL1Cache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(5))
                .maximumSize(10_000)
                .build();
    }
}

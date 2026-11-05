package com.nchuy099.ecom_micro_product_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EcomMicroProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcomMicroProductServiceApplication.class, args);
    }

}

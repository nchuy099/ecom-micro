package com.nchuy099.ecom_micro_service_discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EcomMicroServiceDiscoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcomMicroServiceDiscoveryApplication.class, args);
    }

}

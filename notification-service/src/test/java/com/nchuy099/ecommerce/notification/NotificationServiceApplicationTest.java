package com.nchuy099.ecommerce.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification_service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "eureka.client.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
class NotificationServiceApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that notification-service context starts with JPA and Kafka listener configured
    }
}

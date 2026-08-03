package com.nchuy099.ecommerce.order.client.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceLoggingConfig {
    private static final Logger log = LoggerFactory.getLogger(ResilienceLoggingConfig.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ResilienceLoggingConfig(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostConstruct
    public void registerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerListener);
        circuitBreakerRegistry.getEventPublisher().onEntryAdded(entryAddedEvent ->
                registerListener(entryAddedEvent.getAddedEntry()));
    }

    private void registerListener(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.info("CircuitBreaker '{}' transitioned from {} to {}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }
}

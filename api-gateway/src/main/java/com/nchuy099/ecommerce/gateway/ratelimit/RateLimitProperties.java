package com.nchuy099.ecommerce.gateway.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    private Policy login = new Policy(true, 5, Duration.ofMinutes(15));
    private Policy orders = new Policy(true, 10, Duration.ofSeconds(1));
    private Policy flashSale = new Policy(true, 5, Duration.ofSeconds(1));

    public Policy getLogin() {
        return login;
    }

    public void setLogin(Policy login) {
        this.login = login;
    }

    public Policy getOrders() {
        return orders;
    }

    public void setOrders(Policy orders) {
        this.orders = orders;
    }

    public Policy getFlashSale() {
        return flashSale;
    }

    public void setFlashSale(Policy flashSale) {
        this.flashSale = flashSale;
    }

    public static class Policy {
        private boolean enabled;
        private int capacity;
        private Duration refillPeriod;

        public Policy() {
        }

        public Policy(boolean enabled, int capacity, Duration refillPeriod) {
            this.enabled = enabled;
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(Duration refillPeriod) {
            this.refillPeriod = refillPeriod;
        }
    }
}

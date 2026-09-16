package com.nchuy099.ecommerce.notification.config;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.nchuy099.ecommerce.notification.event.NotificationChannel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {
    private Worker worker = new Worker();
    private Mock mock = new Mock();

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Mock getMock() {
        return mock;
    }

    public void setMock(Mock mock) {
        this.mock = mock;
    }

    public static class Worker {
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(25);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBackoff() {
            return backoff;
        }

        public void setBackoff(Duration backoff) {
            this.backoff = backoff;
        }
    }

    public static class Mock {
        private Set<String> failCampaignIds = new HashSet<>();
        private Set<NotificationChannel> failChannels = new HashSet<>();

        public Set<String> getFailCampaignIds() {
            return failCampaignIds;
        }

        public void setFailCampaignIds(Set<String> failCampaignIds) {
            this.failCampaignIds = failCampaignIds;
        }

        public Set<NotificationChannel> getFailChannels() {
            return failChannels;
        }

        public void setFailChannels(Set<NotificationChannel> failChannels) {
            this.failChannels = failChannels;
        }
    }
}

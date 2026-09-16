package com.nchuy099.ecommerce.notification.provider;

import com.nchuy099.ecommerce.notification.event.NotificationChannelRequestedEvent;
import com.nchuy099.ecommerce.notification.config.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MockNotificationProvider implements NotificationProvider {
    private static final Logger log = LoggerFactory.getLogger(MockNotificationProvider.class);

    private final NotificationProperties properties;

    @Override
    public void send(NotificationChannelRequestedEvent event) {
        if (properties.getMock().getFailCampaignIds().contains(event.campaignId())
                || properties.getMock().getFailChannels().contains(event.channel())) {
            throw new IllegalStateException("Mock " + event.channel() + " provider failed for " + event.campaignId());
        }
        log.info("Mock {} notification sent: campaignId={}, userId={}",
                event.channel(), event.campaignId(), event.userId());
    }
}

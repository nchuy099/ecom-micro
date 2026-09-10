package com.nchuy099.ecommerce.notification.provider;

import com.nchuy099.ecommerce.common.event.NotificationChannelRequestedEvent;

public interface NotificationProvider {
    void send(NotificationChannelRequestedEvent event);
}

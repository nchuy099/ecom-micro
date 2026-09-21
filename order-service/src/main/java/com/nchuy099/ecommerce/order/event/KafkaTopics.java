package com.nchuy099.ecommerce.order.event;

public final class KafkaTopics {
    public static final String ORDER_EVENTS = "order.events";
    public static final String NOTIFICATION_TASKS = "notification.tasks";
    // Kept for source compatibility with the legacy notification event contract.
    public static final String NOTIFICATION_REQUESTED = "notification.requested";

    private KafkaTopics() {
    }
}

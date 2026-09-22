package com.nchuy099.ecommerce.notification.event;

public final class KafkaTopics {
    public static final String ORDER_EVENTS = "order.events";
    public static final String NOTIFICATION_TASKS = "notification.tasks";
    public static final String NOTIFICATION_RETRY = "notification.retry";
    public static final String NOTIFICATION_DLQ = "notification.dlq";
    public static final String NOTIFICATION_REQUESTED = "notification.requested";
    public static final String NOTIFICATION_EMAIL_REQUESTED = "notification.email.requested";
    public static final String NOTIFICATION_PUSH_REQUESTED = "notification.push.requested";
    public static final String NOTIFICATION_SMS_REQUESTED = "notification.sms.requested";
    public static final String NOTIFICATION_EMAIL_DLQ = "notification.email.dlq";
    public static final String NOTIFICATION_PUSH_DLQ = "notification.push.dlq";
    public static final String NOTIFICATION_SMS_DLQ = "notification.sms.dlq";

    private KafkaTopics() {}
}

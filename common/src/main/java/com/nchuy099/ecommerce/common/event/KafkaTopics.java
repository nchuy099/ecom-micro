package com.nchuy099.ecommerce.common.event;

public final class KafkaTopics {
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String STOCK_RESERVED = "stock.reserved";
    public static final String STOCK_RESERVATION_FAILED = "stock.reservation.failed";
    public static final String STOCK_RELEASED = "stock.released";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String NOTIFICATION_REQUESTED = "notification.requested";
    public static final String NOTIFICATION_EMAIL_REQUESTED = "notification.email.requested";
    public static final String NOTIFICATION_PUSH_REQUESTED = "notification.push.requested";
    public static final String NOTIFICATION_SMS_REQUESTED = "notification.sms.requested";
    public static final String NOTIFICATION_EMAIL_DLQ = "notification.email.dlq";
    public static final String NOTIFICATION_PUSH_DLQ = "notification.push.dlq";
    public static final String NOTIFICATION_SMS_DLQ = "notification.sms.dlq";

    private KafkaTopics() {}
}

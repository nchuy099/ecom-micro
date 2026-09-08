package com.nchuy099.ecommerce.notification.exception;

public class NotificationDlqNotFoundException extends RuntimeException {
    public NotificationDlqNotFoundException(Long id) {
        super("Notification DLQ entry not found: " + id);
    }
}

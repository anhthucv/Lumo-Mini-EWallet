package com.chethu.paymentledgerservice.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long notificationId) {
        super("Notification with " + notificationId + " not found");
    }
}

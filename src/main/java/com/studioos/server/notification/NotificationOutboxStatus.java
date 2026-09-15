package com.studioos.server.notification;

public enum NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    RETRY,
    SENT,
    FAILED
}

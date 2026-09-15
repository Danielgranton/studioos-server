package com.studioos.server.notification;

public record NotificationDeliveryResult(boolean accepted, String providerMessageId) {
}

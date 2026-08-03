package com.studioos.server.notification.dto;

import com.studioos.server.shared.enums.NotificationType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateNotificationPreferenceRequest {
    @NotNull
    private NotificationType notificationType;

    private Boolean inAppEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
}

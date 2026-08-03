package com.studioos.server.notification.dto;

import com.studioos.server.shared.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {
    private NotificationType notificationType;
    private boolean inAppEnabled;
    private boolean emailEnabled;
    private boolean smsEnabled;
}

package com.studioos.server.user;

import java.time.LocalDateTime;
import java.util.List;

import com.studioos.server.notification.dto.NotificationPreferenceResponse;
import com.studioos.server.user.dto.PrivacySettingsResponse;
import com.studioos.server.user.dto.UserProfileResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountDataExportResponse {
    LocalDateTime exportedAt;
    UserProfileResponse profile;
    PrivacySettingsResponse privacySettings;
    List<NotificationPreferenceResponse> notificationPreferences;
    List<ExportSession> sessions;
    List<ExportAuditEntry> auditHistory;

    @Value
    @Builder
    public static class ExportSession {
        String id;
        String deviceId;
        String deviceName;
        String userAgent;
        String ipAddress;
        LocalDateTime createdAt;
        LocalDateTime expiresAt;
        LocalDateTime revokedAt;
    }

    @Value
    @Builder
    public static class ExportAuditEntry {
        String eventType;
        String entityId;
        String entityType;
        String description;
        String ipAddress;
        String userAgent;
        LocalDateTime createdAt;
    }
}

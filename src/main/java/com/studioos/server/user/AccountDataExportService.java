package com.studioos.server.user;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.session.RefreshSessionRepository;
import com.studioos.server.notification.NotificationPreferenceService;
import com.studioos.server.payment.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountDataExportService {

    private final UserService userService;
    private final PrivacySettingsService privacySettingsService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final RefreshSessionRepository refreshSessionRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public AccountDataExportResponse export(User currentUser) {
        return AccountDataExportResponse.builder()
                .exportedAt(LocalDateTime.now())
                .profile(userService.getMyProfile(currentUser))
                .privacySettings(privacySettingsService.get(currentUser))
                .notificationPreferences(notificationPreferenceService.getPreferences(currentUser))
                .sessions(refreshSessionRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                        .map(session -> AccountDataExportResponse.ExportSession.builder()
                                .id(session.getId())
                                .deviceId(session.getDeviceId())
                                .deviceName(session.getDeviceName())
                                .userAgent(session.getUserAgent())
                                .ipAddress(session.getIpAddress())
                                .createdAt(session.getCreatedAt())
                                .expiresAt(session.getExpiresAt())
                                .revokedAt(session.getRevokedAt())
                                .build())
                        .toList())
                .auditHistory(auditLogRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                        .map(log -> AccountDataExportResponse.ExportAuditEntry.builder()
                                .eventType(log.getEventType().name())
                                .entityId(log.getEntityId())
                                .entityType(log.getEntityType())
                                .description(log.getDescription())
                                .ipAddress(log.getIpAddress())
                                .userAgent(log.getUserAgent())
                                .createdAt(log.getCreatedAt())
                                .build())
                        .toList())
                .build();
    }
}

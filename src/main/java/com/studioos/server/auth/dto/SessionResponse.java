package com.studioos.server.auth.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private String sessionId;
    private Integer userId;
    private String deviceId;
    private String deviceName;
    private String userAgent;
    private String ipAddress;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    private LocalDateTime lastActiveAt;
    private boolean currentSession;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private boolean active;
}

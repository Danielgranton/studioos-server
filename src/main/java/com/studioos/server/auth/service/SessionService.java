package com.studioos.server.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.studioos.server.auth.dto.LogoutRequest;
import com.studioos.server.auth.dto.SessionResponse;
import com.studioos.server.auth.session.RefreshSession;
import com.studioos.server.auth.session.RefreshSessionRepository;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;

    @Transactional
    public void recordSession(User user, String refreshToken) {
        if (user == null || refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        RequestMetadata metadata = captureRequestMetadata();
        String tokenHash = hash(refreshToken);
        if (refreshSessionRepository.findByTokenHash(tokenHash).isPresent()) {
            return;
        }

        refreshSessionRepository.save(RefreshSession.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .tokenHash(tokenHash)
                .tokenVersion(user.getRefreshTokenVersion() == null ? 0 : user.getRefreshTokenVersion())
                .expiresAt(tokenService.extractExpiration(refreshToken).toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .createdAt(LocalDateTime.now())
                .deviceId(UUID.randomUUID().toString())
                .deviceName(metadata.deviceName())
                .userAgent(metadata.userAgent())
                .ipAddress(metadata.ipAddress())
                .deviceType(metadata.deviceType())
                .browser(metadata.browser())
                .operatingSystem(metadata.operatingSystem())
                .lastActiveAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void rotateSession(User user, String oldRefreshToken, String newRefreshToken) {
        revokeCurrentSession(oldRefreshToken);
        recordSession(user, newRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        if (!tokenService.isRefreshToken(request.getRefreshToken())) {
            throw StudioosException.badRequest("Invalid refresh token");
        }

        String email = tokenService.extractEmail(request.getRefreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> StudioosException.notFound("User not found"));

        if (request.isAllDevices()) {
            logoutAllDevices(user);
            return;
        }

        revokeCurrentSession(request.getRefreshToken());
    }

    @Transactional
    public boolean isSessionActive(String refreshToken) {
        return refreshSessionRepository.findByTokenHashAndRevokedAtIsNull(hash(refreshToken))
                .map(session -> {
                    session.setLastActiveAt(LocalDateTime.now());
                    refreshSessionRepository.save(session);
                    return session.getExpiresAt() != null && session.getExpiresAt().isAfter(LocalDateTime.now());
                }).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listActiveSessions(User user, String refreshToken) {
        if (user == null) {
            throw StudioosException.unauthorized("Authentication required");
        }
        return refreshSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(session -> toResponse(session, refreshToken != null && session.getTokenHash().equals(hash(refreshToken))))
                .toList();
    }

    @Transactional
    public void revokeSession(User user, String sessionId) {
        if (user == null) {
            throw StudioosException.unauthorized("Authentication required");
        }
        RefreshSession session = refreshSessionRepository.findById(sessionId)
                .orElseThrow(() -> StudioosException.notFound("Session not found"));
        if (!session.getUserId().equals(user.getId())) {
            throw StudioosException.forbidden("You cannot revoke another user's session");
        }
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(LocalDateTime.now());
            refreshSessionRepository.save(session);
        }
    }

    @Transactional
    public void logoutAllDevices(User user) {
        int currentVersion = user.getRefreshTokenVersion() == null ? 0 : user.getRefreshTokenVersion();
        user.setRefreshTokenVersion(currentVersion + 1);
        userRepository.save(user);

        List<RefreshSession> activeSessions = refreshSessionRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        LocalDateTime now = LocalDateTime.now();
        activeSessions.forEach(session -> session.setRevokedAt(now));
        refreshSessionRepository.saveAll(activeSessions);
    }

    @Transactional
    public void revokeCurrentSession(String refreshToken) {
        String tokenHash = hash(refreshToken);
        RefreshSession session = refreshSessionRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> StudioosException.unauthorized("Refresh token has been revoked, please login again"));
        session.setRevokedAt(LocalDateTime.now());
        refreshSessionRepository.save(session);
        log.debug("Revoked refresh session {}", session.getId());
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }

    private SessionResponse toResponse(RefreshSession session, boolean currentSession) {
        String browser = firstNonBlank(session.getBrowser(), detectBrowser(session.getUserAgent()));
        String operatingSystem = firstNonBlank(session.getOperatingSystem(), detectOperatingSystem(session.getUserAgent()));
        String deviceType = firstNonBlank(session.getDeviceType(), detectDeviceType(session.getUserAgent()));
        String deviceName = firstNonBlank(session.getDeviceName(), browser + " on " + operatingSystem);
        return SessionResponse.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .deviceId(session.getDeviceId())
                .deviceName(deviceName)
                .userAgent(session.getUserAgent())
                .ipAddress(session.getIpAddress())
                .deviceType(deviceType)
                .browser(browser)
                .operatingSystem(operatingSystem)
                .lastActiveAt(firstNonBlankDate(session.getLastActiveAt(), session.getCreatedAt()))
                .currentSession(currentSession)
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .revokedAt(session.getRevokedAt())
                .active(session.getRevokedAt() == null
                        && session.getExpiresAt() != null
                        && session.getExpiresAt().isAfter(LocalDateTime.now()))
                .build();
    }

    private RequestMetadata captureRequestMetadata() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return RequestMetadata.empty();
        }

        HttpServletRequest request = servletAttributes.getRequest();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = firstNonBlank(
                request.getHeader("X-Forwarded-For"),
                request.getRemoteAddr());

        String browser = detectBrowser(userAgent);
        String operatingSystem = detectOperatingSystem(userAgent);
        String deviceType = detectDeviceType(userAgent);
        return new RequestMetadata(browser + " on " + operatingSystem, userAgent, ipAddress, deviceType, browser, operatingSystem);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String detectBrowser(String userAgent) {
        String value = userAgent == null ? "Unknown browser" : userAgent.toLowerCase(Locale.ROOT);
        if (value.contains("edg/")) return "Edge";
        if (value.contains("chrome/") && !value.contains("edg/")) return "Chrome";
        if (value.contains("firefox/")) return "Firefox";
        if (value.contains("safari/") && !value.contains("chrome/")) return "Safari";
        if (value.contains("curl")) return "CLI client";
        return "Other client";
    }

    private String detectOperatingSystem(String userAgent) {
        String value = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (value.contains("android")) return "Android";
        if (value.contains("iphone") || value.contains("ipad")) return "iOS";
        if (value.contains("windows")) return "Windows";
        if (value.contains("mac os")) return "macOS";
        if (value.contains("linux")) return "Linux";
        return "Other OS";
    }

    private String detectDeviceType(String userAgent) {
        String value = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (value.isBlank()) return "UNKNOWN";
        return value.contains("mobile") || value.contains("android") || value.contains("iphone") ? "MOBILE" : "DESKTOP";
    }

    private LocalDateTime firstNonBlankDate(LocalDateTime value, LocalDateTime fallback) {
        return value == null ? fallback : value;
    }

    private record RequestMetadata(String deviceName, String userAgent, String ipAddress, String deviceType, String browser, String operatingSystem) {
        static RequestMetadata empty() {
            return new RequestMetadata("Unknown device", null, null, "UNKNOWN", "Unknown browser", "Unknown OS");
        }
    }
}

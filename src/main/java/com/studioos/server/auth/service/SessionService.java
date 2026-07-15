package com.studioos.server.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.dto.LogoutRequest;
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

    public boolean isSessionActive(String refreshToken) {
        return refreshSessionRepository.findByTokenHashAndRevokedAtIsNull(hash(refreshToken)).isPresent();
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
}

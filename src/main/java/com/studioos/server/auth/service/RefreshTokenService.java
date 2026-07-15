package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.RefreshTokenRequest;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final TokenService tokenService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        final String refreshToken = request.getRefreshToken();
        if (!tokenService.isRefreshToken(refreshToken)) {
            throw StudioosException.unauthorized("Invalid refresh token");
        }

        final String email = tokenService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> StudioosException.notFound("User not found"));

        if (tokenService.isExpired(refreshToken)) {
            throw StudioosException.unauthorized("Refresh token expired, please login again");
        }

        Integer tokenVersion = tokenService.extractTokenVersion(refreshToken);
        int currentVersion = user.getRefreshTokenVersion() == null ? 0 : user.getRefreshTokenVersion();
        if (tokenVersion == null || tokenVersion != currentVersion) {
            throw StudioosException.unauthorized("Refresh token has been revoked, please login again");
        }

        if (!sessionService.isSessionActive(refreshToken)) {
            throw StudioosException.unauthorized("Refresh token has been revoked, please login again");
        }

        AuthResponse response = tokenService.issue(user);
        sessionService.rotateSession(user, refreshToken, response.getRefreshToken());
        return response;
    }
}

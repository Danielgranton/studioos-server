package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;

import com.studioos.server.auth.JwtService;
import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.user.User;

import java.util.Date;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;

    public AuthResponse issue(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

    public boolean isRefreshToken(String token) {
        return jwtService.isTokenOfType(token, JwtService.TOKEN_TYPE_REFRESH);
    }

    public boolean isExpired(String token) {
        return jwtService.isTokenExpired(token);
    }

    public String extractEmail(String token) {
        return jwtService.extractEmail(token);
    }

    public Integer extractTokenVersion(String token) {
        return jwtService.extractTokenVersion(token);
    }

    public Date extractExpiration(String token) {
        return jwtService.extractExpiration(token);
    }
}

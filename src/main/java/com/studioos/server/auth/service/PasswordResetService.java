package com.studioos.server.auth.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.communication.CommunicationClient;
import com.studioos.server.auth.communication.CommunicationRequestFactory;
import com.studioos.server.auth.dto.ChangePasswordRequest;
import com.studioos.server.auth.password.PasswordResetToken;
import com.studioos.server.auth.password.PasswordResetTokenRepository;
import com.studioos.server.auth.dto.ForgotPasswordRequest;
import com.studioos.server.auth.dto.LoginRequest;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.ResetPasswordRequest;
import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserLookupService userLookupService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final CommunicationClient communicationClient;
    private final CommunicationRequestFactory communicationRequestFactory;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public OtpSentResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userLookupService.findByIdentifier(request.getIdentifier());
        if (!passwordService.hasPassword(user)) {
            throw StudioosException.badRequest("This account does not use password login");
        }

        String token = generateToken();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(hash(token))
                .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build());

        communicationClient.send(communicationRequestFactory.passwordReset(user.getEmail(), user.getPhone(), token));

        return OtpSentResponse.builder()
                .message("Password reset instructions sent")
                .maskedEmail(maskEmail(user.getEmail()))
                .maskedPhone(maskPhone(user.getPhone()))
                .build();
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findTopByTokenHashAndUsedFalseOrderByCreatedAtDesc(hash(request.getToken()))
                .orElseThrow(() -> StudioosException.badRequest("Invalid or expired password reset token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw StudioosException.badRequest("Invalid or expired password reset token");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> StudioosException.notFound("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        sessionService.logoutAllDevices(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        AuthResponse response = tokenService.issue(user);
        sessionService.recordSession(user, response.getRefreshToken());
        return response;
    }

    @Transactional
    public AuthResponse changePassword(User user, ChangePasswordRequest request) {
        if (user == null) {
            throw StudioosException.unauthorized("Authentication required");
        }
        if (passwordService.hasPassword(user)
                && !passwordService.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw StudioosException.badRequest("Current password is invalid");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        sessionService.logoutAllDevices(user);
        AuthResponse response = tokenService.issue(user);
        sessionService.recordSession(user, response.getRefreshToken());
        return response;
    }

    @Transactional
    public AuthResponse loginWithPassword(LoginRequest request) {
        User user = userLookupService.findByIdentifier(request.getIdentifier());
        if (!user.isAccountVerified()) {
            throw StudioosException.badRequest("Account is not verified yet");
        }
        if (!passwordService.hasPassword(user)) {
            throw StudioosException.badRequest("This account does not use password login");
        }
        if (!passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            throw StudioosException.badRequest("Invalid credentials");
        }
        AuthResponse response = tokenService.issue(user);
        sessionService.recordSession(user, response.getRefreshToken());
        return response;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash token", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1];
    }

    private String maskPhone(String phone) {
        if (phone == null) return null;
        return phone.substring(0, Math.min(4, phone.length())) + "****";
    }
}

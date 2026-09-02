package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.EmailChangeRequest;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.communication.CommunicationClient;
import com.studioos.server.communication.CommunicationRequestFactory;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailChangeService {

    private final OtpService otpService;
    private final CommunicationClient communicationClient;
    private final CommunicationRequestFactory communicationRequestFactory;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final SessionService sessionService;

    @Transactional
    public OtpSentResponse request(User user, EmailChangeRequest request) {
        requireUser(user);
        String newEmail = normalize(request == null ? null : request.getNewEmail());
        validateEmail(newEmail);

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw StudioosException.badRequest("This is already your account email");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw StudioosException.conflict("Email is already in use");
        }

        String code = otpService.generateAndSave(otpKey(user, newEmail));
        communicationClient.send(communicationRequestFactory.otp(newEmail, null, code));
        return OtpSentResponse.builder()
                .message("Email change verification code sent")
                .maskedEmail(maskEmail(newEmail))
                .build();
    }

    @Transactional
    public AuthResponse verify(User user, VerifyOtpRequest request) {
        requireUser(user);
        String newEmail = normalize(request == null ? null : request.getIdentifier());
        validateEmail(newEmail);

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw StudioosException.badRequest("This is already your account email");
        }
        otpService.verify(otpKey(user, newEmail), request.getCode());

        if (userRepository.existsByEmail(newEmail)) {
            throw StudioosException.conflict("Email is already in use");
        }

        user.setEmail(newEmail);
        user.setEmailVerified(true);
        userRepository.save(user);

        // Existing JWT subjects contain the old email, so rotate all sessions.
        sessionService.logoutAllDevices(user);
        AuthResponse auth = tokenService.issue(user);
        sessionService.recordSession(user, auth.getRefreshToken());
        return auth;
    }

    private void requireUser(User user) {
        if (user == null) throw StudioosException.unauthorized("Authentication required");
    }

    private String otpKey(User user, String email) {
        return "email-change:" + user.getId() + ":" + email;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void validateEmail(String email) {
        if (email.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw StudioosException.badRequest("Enter a valid email address");
        }
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@", 2);
        return parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1];
    }
}

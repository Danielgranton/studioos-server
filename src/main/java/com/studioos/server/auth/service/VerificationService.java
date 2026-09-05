package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;
import com.studioos.server.shared.enums.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final UserLookupService userLookupService;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponse verify(VerifyOtpRequest request) {
        return verifyRegistration(request);
    }

    @Transactional
    public AuthResponse verifyLogin(VerifyOtpRequest request) {
        User user = userLookupService.findByIdentifier(request.getIdentifier());
        otpService.verify(user.getEmail(), request.getCode());
        return issueSession(user);
    }

    @Transactional
    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        User user = userLookupService.findByIdentifier(request.getIdentifier());
        otpService.verify(user.getEmail(), request.getCode());

        // Registration OTPs are email-keyed; do not claim phone verification too.
        user.setEmailVerified(true);
        user.setAccountVerified(true);
        userRepository.save(user);
        return issueSession(user);
    }

    @Transactional
    public AuthResponse googleLogin(String subject, String email, String name) {
        User user = userRepository.findByGoogleSubject(subject).orElseGet(() ->
                userRepository.findByEmail(email).map(existing -> {
                    if (existing.getGoogleSubject() != null && !subject.equals(existing.getGoogleSubject())) {
                        throw com.studioos.server.shared.exceptions.StudioosException.conflict("This account is linked to another Google identity");
                    }
                    existing.setGoogleSubject(subject);
                    return existing;
                }).orElseGet(() -> User.builder()
                        .email(email)
                        .name(name == null || name.isBlank() ? email : name)
                        .googleSubject(subject)
                        .role(Role.USER)
                        .emailVerified(true)
                        .accountVerified(true)
                        .build()));
        user.setEmailVerified(true);
        user.setAccountVerified(true);
        userRepository.save(user);
        return issueSession(user);
    }

    private AuthResponse issueSession(User user) {
        sessionService.logoutAllDevices(user);
        AuthResponse response = tokenService.issue(user);
        sessionService.recordSession(user, response.getRefreshToken());
        return response;
    }
}

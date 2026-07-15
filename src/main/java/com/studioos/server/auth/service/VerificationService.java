package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

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
        User user = userLookupService.findByIdentifier(request.getIdentifier());
        otpService.verify(user.getEmail(), request.getCode());

        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setAccountVerified(true);
        userRepository.save(user);
        sessionService.logoutAllDevices(user);

        AuthResponse response = tokenService.issue(user);
        sessionService.recordSession(user, response.getRefreshToken());
        return response;
    }

    @Transactional
    public AuthResponse verifyLogin(VerifyOtpRequest request) {
        return verify(request);
    }

    @Transactional
    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        return verify(request);
    }
}

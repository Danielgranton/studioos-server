package com.studioos.server.auth;

import org.springframework.stereotype.Service;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.LoginRequest;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.RefreshTokenRequest;
import com.studioos.server.auth.dto.RegisterRequest;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.service.LoginService;
import com.studioos.server.auth.service.RefreshTokenService;
import com.studioos.server.auth.service.RegistrationService;
import com.studioos.server.auth.service.SessionService;
import com.studioos.server.auth.service.VerificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final VerificationService verificationService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;

    public OtpSentResponse register(RegisterRequest request) {
        return registrationService.register(request);
    }

    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        return verificationService.verifyRegistration(request);
    }

    public OtpSentResponse login(LoginRequest request) {
        return loginService.login(request);
    }

    public AuthResponse verifyLogin(VerifyOtpRequest request) {
        return verificationService.verifyLogin(request);
    }

    public OtpSentResponse resendOtp(LoginRequest request) {
        return loginService.resendOtp(request);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenService.refresh(request);
    }

    public void logout(com.studioos.server.auth.dto.LogoutRequest request) {
        sessionService.logout(request);
    }
}

package com.studioos.server.auth;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.otp.OtpRateLimitService;
import com.studioos.server.auth.service.VerificationService;
import com.studioos.server.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final OtpRateLimitService otpRateLimitService;
    private final AuthCookieService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistration(
            @Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        otpRateLimitService.checkVerification(httpRequest);
        AuthResponse response = verificationService.verifyRegistration(request);
        authCookieService.addAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success("Registration complete", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLogin(
            @Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        otpRateLimitService.checkVerification(httpRequest);
        AuthResponse response = verificationService.verifyLogin(request);
        authCookieService.addAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}

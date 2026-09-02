package com.studioos.server.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.PhoneChangeRequest;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.otp.OtpRateLimitService;
import com.studioos.server.auth.service.PhoneChangeService;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/phone-change")
@RequiredArgsConstructor
public class PhoneChangeController {

    private final PhoneChangeService phoneChangeService;
    private final AuthCookieService authCookieService;
    private final OtpRateLimitService otpRateLimitService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<OtpSentResponse>> request(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PhoneChangeRequest request,
            HttpServletRequest httpRequest) {
        otpRateLimitService.checkOtpRequest(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(phoneChangeService.request(user, request)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verify(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        otpRateLimitService.checkVerification(httpRequest);
        AuthResponse auth = phoneChangeService.verify(user, request);
        authCookieService.addAuthCookies(httpResponse, auth);
        return ResponseEntity.ok(ApiResponse.success("Phone number changed successfully", auth));
    }
}

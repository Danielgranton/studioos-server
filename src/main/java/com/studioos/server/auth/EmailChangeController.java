package com.studioos.server.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.EmailChangeRequest;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.service.EmailChangeService;
import com.studioos.server.auth.otp.OtpRateLimitService;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/email-change")
@RequiredArgsConstructor
public class EmailChangeController {

    private final EmailChangeService emailChangeService;
    private final AuthCookieService authCookieService;
    private final OtpRateLimitService otpRateLimitService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<OtpSentResponse>> request(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody EmailChangeRequest request,
            HttpServletRequest httpRequest) {
        otpRateLimitService.checkOtpRequest(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(emailChangeService.request(user, request)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verify(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        otpRateLimitService.checkVerification(httpRequest);
        AuthResponse auth = emailChangeService.verify(user, request);
        authCookieService.addAuthCookies(httpResponse, auth);
        return ResponseEntity.ok(ApiResponse.success("Email changed successfully", auth));
    }
}

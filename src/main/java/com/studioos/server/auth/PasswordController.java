package com.studioos.server.auth;

import com.studioos.server.auth.dto.*;
import com.studioos.server.auth.service.PasswordResetService;
import com.studioos.server.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordResetService passwordResetService;
    private final AuthCookieService authCookieService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = passwordResetService.loginWithPassword(request);
        authCookieService.addAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<OtpSentResponse>> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        OtpSentResponse response = passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset sent", response));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<AuthResponse>> reset(
            @Valid @RequestBody ResetPasswordRequest request, HttpServletResponse httpResponse) {
        AuthResponse response = passwordResetService.resetPassword(request);
        authCookieService.addAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", response));
    }

    @PostMapping("/change")
    public ResponseEntity<ApiResponse<AuthResponse>> change(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse httpResponse) {
        AuthResponse response = passwordResetService.changePassword((com.studioos.server.user.User) authentication.getPrincipal(), request);
        authCookieService.addAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success("Password changed", response));
    }
}

package com.studioos.server.auth;

import com.studioos.server.auth.dto.*;
import com.studioos.server.auth.service.PasswordResetService;
import com.studioos.server.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = passwordResetService.loginWithPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot")
    public ResponseEntity<ApiResponse<OtpSentResponse>> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        OtpSentResponse response = passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset sent", response));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<AuthResponse>> reset(@Valid @RequestBody ResetPasswordRequest request) {
        AuthResponse response = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", response));
    }

    @PostMapping("/change")
    public ResponseEntity<ApiResponse<AuthResponse>> change(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthResponse response = passwordResetService.changePassword((com.studioos.server.user.User) authentication.getPrincipal(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed", response));
    }
}

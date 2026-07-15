package com.studioos.server.auth;

import com.studioos.server.auth.dto.*;
import com.studioos.server.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─── Registration ───
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OtpSentResponse>> register(@Valid @RequestBody RegisterRequest request) {
        OtpSentResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("OTP sent successfully", response));
    }

    // ─── Login ───
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OtpSentResponse>> login(@Valid @RequestBody LoginRequest request) {
        OtpSentResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", response));
    }

    // ─── Resend OTP ───
    @PostMapping("/otp/resend")
    public ResponseEntity<ApiResponse<OtpSentResponse>> resendOtp(@Valid @RequestBody LoginRequest request) {
        OtpSentResponse response = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP resent", response));
    }

    // ─── Refresh token ───
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out", null));
    }
}

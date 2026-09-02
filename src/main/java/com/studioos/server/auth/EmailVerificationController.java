package com.studioos.server.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.service.EmailVerificationService;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<OtpSentResponse>> request(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                emailVerificationService.request(user)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyOtpRequest request) {
        emailVerificationService.verify(user, request);
        return ResponseEntity.ok(ApiResponse.success("Email verified", null));
    }
}

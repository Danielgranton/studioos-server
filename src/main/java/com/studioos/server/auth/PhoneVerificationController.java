package com.studioos.server.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.service.PhoneVerificationService;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/phone-verification")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<OtpSentResponse>> request(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(phoneVerificationService.request(user)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyOtpRequest request) {
        phoneVerificationService.verify(user, request);
        return ResponseEntity.ok(ApiResponse.success("Phone verified", null));
    }
}

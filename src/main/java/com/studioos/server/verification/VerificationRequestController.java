package com.studioos.server.verification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class VerificationRequestController {

    private final VerificationReviewService verificationReviewService;

    @PostMapping("/users/me/verification/request")
    public ResponseEntity<ApiResponse<Void>> requestUserVerification(
            @AuthenticationPrincipal User user
    ) {
        verificationReviewService.requestUserVerification(user);
        return ResponseEntity.ok(ApiResponse.success("Verification request submitted"));
    }

    @PostMapping("/studios/{studioId}/verification/request")
    public ResponseEntity<ApiResponse<Void>> requestStudioVerification(
            @AuthenticationPrincipal User user,
            @PathVariable String studioId
    ) {
        verificationReviewService.requestStudioVerification(user, studioId);
        return ResponseEntity.ok(ApiResponse.success("Studio verification request submitted"));
    }
}

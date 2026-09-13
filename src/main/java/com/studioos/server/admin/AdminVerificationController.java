package com.studioos.server.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.enums.VerificationStatus;
import com.studioos.server.verification.VerificationReviewService;
import com.studioos.server.verification.dto.VerificationCandidateResponse;
import com.studioos.server.verification.dto.VerificationReviewRequest;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/verification")
@RequiredArgsConstructor
@Validated
public class AdminVerificationController {

    private final VerificationReviewService verificationReviewService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<VerificationCandidateResponse>>> pendingUsers() {
        return ResponseEntity.ok(ApiResponse.success(verificationReviewService.pendingUsers()));
    }

    @GetMapping("/studios")
    public ResponseEntity<ApiResponse<List<VerificationCandidateResponse>>> pendingStudios() {
        return ResponseEntity.ok(ApiResponse.success(verificationReviewService.pendingStudios()));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<VerificationStatus>> reviewUser(
            @AuthenticationPrincipal User reviewer,
            @PathVariable Integer userId,
            @Valid @RequestBody VerificationReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("User verification updated",
                verificationReviewService.reviewUser(reviewer, userId, request)));
    }

    @PutMapping("/studios/{studioId}")
    public ResponseEntity<ApiResponse<VerificationStatus>> reviewStudio(
            @AuthenticationPrincipal User reviewer,
            @PathVariable String studioId,
            @Valid @RequestBody VerificationReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Studio verification updated",
                verificationReviewService.reviewStudio(reviewer, studioId, request)));
    }
}

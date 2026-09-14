package com.studioos.server.reviews;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.reviews.dto.ReviewCommentRequest;
import com.studioos.server.reviews.dto.ReviewCommentResponse;
import com.studioos.server.reviews.dto.ReviewInteractionResponse;
import com.studioos.server.reviews.dto.ReviewReactionRequest;
import com.studioos.server.reviews.dto.ReviewReportRequest;
import com.studioos.server.reviews.dto.ReviewReportResponse;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reviews/{reviewType}/{reviewId}")
@RequiredArgsConstructor
@Validated
public class ReviewInteractionController {

    private final ReviewInteractionService service;
    private final ReviewModerationService moderationService;

    @PostMapping("/report")
    public ResponseEntity<ApiResponse<ReviewReportResponse>> report(
            @AuthenticationPrincipal User user,
            @PathVariable ReviewType reviewType,
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review reported", moderationService.report(user, reviewType, reviewId, request)));
    }

    @GetMapping("/interactions")
    public ResponseEntity<ApiResponse<ReviewInteractionResponse>> summary(
            @AuthenticationPrincipal User user,
            @PathVariable ReviewType reviewType,
            @PathVariable String reviewId) {
        return ResponseEntity.ok(ApiResponse.success(service.summary(user, reviewType, reviewId)));
    }

    @PostMapping("/reaction")
    public ResponseEntity<ApiResponse<ReviewInteractionResponse>> react(
            @AuthenticationPrincipal User user,
            @PathVariable ReviewType reviewType,
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewReactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.react(user, reviewType, reviewId, request)));
    }

    @DeleteMapping("/reaction")
    public ResponseEntity<ApiResponse<ReviewInteractionResponse>> clearReaction(
            @AuthenticationPrincipal User user,
            @PathVariable ReviewType reviewType,
            @PathVariable String reviewId) {
        return ResponseEntity.ok(ApiResponse.success(service.clearReaction(user, reviewType, reviewId)));
    }

    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<Page<ReviewCommentResponse>>> comments(
            @PathVariable ReviewType reviewType,
            @PathVariable String reviewId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getComments(reviewType, reviewId, pageable)));
    }

    @PostMapping("/comments")
    public ResponseEntity<ApiResponse<ReviewCommentResponse>> addComment(
            @AuthenticationPrincipal User user,
            @PathVariable ReviewType reviewType,
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.addComment(user, reviewType, reviewId, request)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable String commentId) {
        service.deleteComment(user, commentId);
        return ResponseEntity.ok(ApiResponse.success("Review comment deleted"));
    }
}

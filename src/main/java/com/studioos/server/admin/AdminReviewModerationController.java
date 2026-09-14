package com.studioos.server.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.reviews.ReviewModerationService;
import com.studioos.server.reviews.dto.ReviewModerationRequest;
import com.studioos.server.reviews.dto.ReviewReportResponse;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@Validated
public class AdminReviewModerationController {

    private final ReviewModerationService moderationService;

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Page<ReviewReportResponse>>> pendingReports(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(moderationService.getPendingReports(pageable)));
    }

    @PutMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<ReviewReportResponse>> resolveReport(
            @AuthenticationPrincipal User moderator,
            @PathVariable String reportId,
            @Valid @RequestBody ReviewModerationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review report resolved", moderationService.resolve(moderator, reportId, request)));
    }
}

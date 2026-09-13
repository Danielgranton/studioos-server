package com.studioos.server.engagement;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.studioos.server.engagement.dto.EngagementRequest;
import com.studioos.server.engagement.dto.EngagementStateResponse;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/engagement")
@RequiredArgsConstructor
@Validated
public class EngagementController {

    private final EngagementService engagementService;

    @PostMapping("/follows")
    public ResponseEntity<ApiResponse<EngagementStateResponse>> follow(@AuthenticationPrincipal User user, @Valid @RequestBody EngagementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(engagementService.follow(user, request)));
    }

    @DeleteMapping("/follows")
    public ResponseEntity<ApiResponse<EngagementStateResponse>> unfollow(@AuthenticationPrincipal User user, @Valid @RequestBody EngagementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(engagementService.unfollow(user, request)));
    }

    @PostMapping("/favorites")
    public ResponseEntity<ApiResponse<EngagementStateResponse>> favorite(@AuthenticationPrincipal User user, @Valid @RequestBody EngagementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(engagementService.favorite(user, request)));
    }

    @DeleteMapping("/favorites")
    public ResponseEntity<ApiResponse<EngagementStateResponse>> unfavorite(@AuthenticationPrincipal User user, @Valid @RequestBody EngagementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(engagementService.unfavorite(user, request)));
    }

    @PostMapping("/views")
    public ResponseEntity<ApiResponse<EngagementStateResponse>> view(@AuthenticationPrincipal User user, @Valid @RequestBody EngagementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(engagementService.recordView(user, request)));
    }
}

package com.studioos.server.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.dashboard.dto.DashboardOverviewResponse;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardOverviewController {

    private final DashboardOverviewService dashboardOverviewService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('USER', 'ARTIST', 'PRODUCER')")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(dashboardOverviewService.getOverview(currentUser)));
    }
}

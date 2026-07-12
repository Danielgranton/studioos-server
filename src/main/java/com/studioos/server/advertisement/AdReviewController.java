package com.studioos.server.advertisement;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.studioos.server.user.User;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/ads")
@RequiredArgsConstructor
public class AdReviewController {

    private final AdReviewService adReviewService;

    @PatchMapping("/{advertisementId}/approve")
    public void approve(@AuthenticationPrincipal User admin, @PathVariable String advertisementId) {
        adReviewService.approve(admin, advertisementId);
    }

    @PatchMapping("/{advertisementId}/reject")
    public void reject(@AuthenticationPrincipal User admin, @PathVariable String advertisementId) {
        adReviewService.reject(admin, advertisementId);
    }
}
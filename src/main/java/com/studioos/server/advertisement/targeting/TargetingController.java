package com.studioos.server.advertisement.targeting;

import com.studioos.server.advertisement.targeting.dto.TargetingResponse;
import com.studioos.server.advertisement.targeting.dto.UpdateTargetingRequest;
import com.studioos.server.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ads/campaigns/{campaignId}/targeting")
public class TargetingController {

    private final TargetingService targetingService;

    @GetMapping
    public Optional<TargetingResponse> getTargeting(
            @AuthenticationPrincipal User advertiser,
            @PathVariable String campaignId) {
        return targetingService.getTargeting(advertiser.getId(), campaignId);
    }

    @PutMapping
    public TargetingResponse updateTargeting(
            @AuthenticationPrincipal User advertiser,
            @PathVariable String campaignId,
            @Valid @RequestBody UpdateTargetingRequest request) {
        return targetingService.upsertTargeting(advertiser.getId(), campaignId, request);
    }
}

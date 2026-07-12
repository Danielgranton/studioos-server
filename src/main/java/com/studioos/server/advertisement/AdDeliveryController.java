package com.studioos.server.advertisement;

import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.studioos.server.advertisement.dto.AdDeliveryResponse;
import com.studioos.server.shared.enums.AdPlacement;
import com.studioos.server.user.User;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdDeliveryController {

    private final AdDeliveryService adDeliveryService;
    private final AdClickService adClickService;

    @GetMapping("/serve")
    public Optional<AdDeliveryResponse> serve(
            @RequestParam AdPlacement placement,
            @AuthenticationPrincipal User user) {
        Integer userId = (user != null) ? user.getId() : null;
        return adDeliveryService.selectAdvertisement(placement, userId);
    }

    @PostMapping("/{advertisementId}/click")
    public String click(
            @PathVariable String advertisementId,
            @AuthenticationPrincipal User user) {
        Integer userId = (user != null) ? user.getId() : null;
        return adClickService.recordClickAndGetRedirectUrl(advertisementId, userId);
    }
}
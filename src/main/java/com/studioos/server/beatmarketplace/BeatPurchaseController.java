package com.studioos.server.beatmarketplace;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.beatmarketplace.dto.BeatPurchaseInitiationResponse;
import com.studioos.server.beatmarketplace.dto.PurchaseBeatRequest;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/beats")
@RequiredArgsConstructor
public class BeatPurchaseController {

    private final BeatPurchaseService beatPurchaseService;

    @PostMapping("/{beatId}/purchase")
    public BeatPurchaseInitiationResponse purchase(
            @AuthenticationPrincipal User buyer,
            @PathVariable String beatId,
            @Valid @RequestBody PurchaseBeatRequest request) {
        return beatPurchaseService.initiatePurchase(buyer.getId(), beatId, request);
    }
}
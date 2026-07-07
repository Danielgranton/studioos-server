package com.studioos.server.beatmarketplace;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.beatmarketplace.dto.BeatLicenseResponse;
import com.studioos.server.beatmarketplace.dto.CreateLicensesRequest;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/beats")
@RequiredArgsConstructor
public class BeatLicenseController {

    private final BeatLicenseService beatLicenseService;

    @PostMapping("/{beatId}/licenses")
    public List<BeatLicenseResponse> createLicenses(
            @AuthenticationPrincipal User producer,
            @PathVariable String beatId,
            @Valid @RequestBody CreateLicensesRequest request) {
        return beatLicenseService.createLicenses(producer.getId(), beatId, request);
    }

    @GetMapping("/{beatId}/licenses")
    public List<BeatLicenseResponse> getLicenses(@PathVariable String beatId) {
        return beatLicenseService.getLicensesForBeat(beatId);
    }
}
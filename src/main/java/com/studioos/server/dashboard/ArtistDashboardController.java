package com.studioos.server.dashboard;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.dashboard.dto.ArtistDashboardResponse;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class ArtistDashboardController {

    private final ArtistDashboardService artistDashboardService;

    @GetMapping("/artist")
    public ArtistDashboardResponse getMyDashboard(@AuthenticationPrincipal User artist) {
        return artistDashboardService.getDashboard(artist.getId());
    }
}
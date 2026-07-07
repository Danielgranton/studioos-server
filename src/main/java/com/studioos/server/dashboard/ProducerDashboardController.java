package com.studioos.server.dashboard;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.dashboard.dto.ProducerDashboardResponse;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class ProducerDashboardController {

    private final ProducerDashboardService producerDashboardService;

    @GetMapping("/producer")
    public ProducerDashboardResponse getMyResponse(@AuthenticationPrincipal User producer) {
        return producerDashboardService.getDashboard(producer.getId());
    }
}
package com.studioos.server.platform;

import com.studioos.server.beatmarketplace.BeatRepository;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.AccountStatus;
import com.studioos.server.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformStatsController {

    private final StudioRepository studioRepository;
    private final BeatRepository beatRepository;
    private final UserRepository userRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getStats() {
        PlatformStatsResponse stats = new PlatformStatsResponse(
                studioRepository.count(),
                userRepository.countByRoleAndStatus(Role.PRODUCER, AccountStatus.ACTIVE),
                beatRepository.countByStatusAndVisibility(BeatStatus.READY, BeatVisibility.PUBLIC),
                userRepository.countByRoleAndStatus(Role.USER, AccountStatus.ACTIVE));

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

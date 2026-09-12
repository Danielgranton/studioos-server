package com.studioos.server.platform;

import com.studioos.server.beatmarketplace.BeatRepository;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.reviews.ProducerReviewRepository;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.user.AccountStatus;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
public class PlatformStatsController {

    private final StudioRepository studioRepository;
    private final BeatRepository beatRepository;
    private final UserRepository userRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final ProducerReviewRepository producerReviewRepository;
    private final PresignedUrlService presignedUrlService;

    @Value("${storage.s3.profile-url-expiry-seconds:3600}")
    private int profileUrlExpirySeconds;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getStats() {
        PlatformStatsResponse stats = new PlatformStatsResponse(
                studioRepository.count(),
                userRepository.countByRoleAndStatus(Role.PRODUCER, AccountStatus.ACTIVE),
                beatRepository.countByStatusAndVisibility(BeatStatus.READY, BeatVisibility.PUBLIC),
                userRepository.countByRoleAndStatus(Role.ARTIST, AccountStatus.ACTIVE),
                studioRepository.countServiceOfferings(),
                adCampaignRepository.count());

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/featured-creators")
    public ResponseEntity<ApiResponse<FeaturedCreatorsResponse>> getFeaturedCreators(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "4") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 4));
        List<Role> roles = List.of(Role.ARTIST, Role.PRODUCER);
        List<FeaturedCreatorResponse> creators = userRepository
                .findFeaturedCreators(roles, AccountStatus.ACTIVE, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toFeaturedCreator)
                .toList();

        FeaturedCreatorsResponse response = new FeaturedCreatorsResponse(
                creators,
                userRepository.countFeaturedCreators(roles, AccountStatus.ACTIVE),
                producerReviewRepository.findAverageRating());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private FeaturedCreatorResponse toFeaturedCreator(User user) {
        return new FeaturedCreatorResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                resolveImageUrl(user.getProfileImageThumbnail()),
                user.isAccountVerified());
    }

    private String resolveImageUrl(String reference) {
        if (reference == null || !reference.startsWith("s3://")) return reference;
        String remainder = reference.substring("s3://".length());
        int separator = remainder.indexOf('/');
        if (separator <= 0 || separator == remainder.length() - 1) return reference;
        return presignedUrlService.generateDownloadUrl(
                remainder.substring(0, separator),
                remainder.substring(separator + 1),
                profileUrlExpirySeconds);
    }
}

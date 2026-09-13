package com.studioos.server.artist;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import com.studioos.server.artist.dto.ArtistBrowseResponse;
import com.studioos.server.engagement.EngagementAction;
import com.studioos.server.engagement.EngagementEdgeRepository;
import com.studioos.server.engagement.EngagementTargetType;
import com.studioos.server.engagement.PopularityService;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.shared.enums.AvailabilityStatus;
import com.studioos.server.shared.enums.VerificationStatus;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.user.AccountStatus;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistBrowseService {

    private final UserRepository userRepository;
    private final ArtistServiceOfferingService offeringService;
    private final ArtistReviewRepository reviewRepository;
    private final EngagementEdgeRepository engagementRepository;
    private final PopularityService popularityService;
    private final PresignedUrlService presignedUrlService;

    @Value("${storage.s3.profile-url-expiry-seconds:3600}")
    private int profileUrlExpirySeconds;

    @Transactional(readOnly = true)
    public PageResponse<ArtistBrowseResponse> getArtists(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        return PageResponse.from(userRepository.findPublicArtists(AccountStatus.ACTIVE, pageable)
                .map(this::toResponse));
    }

    private ArtistBrowseResponse toResponse(User artist) {
        var services = offeringService.getPublicServices(artist.getId());
        Double averageRating = reviewRepository.findAverageRatingByArtistId(artist.getId());
        var specialties = services.stream()
                .map(service -> service.getName())
                .filter(Objects::nonNull)
                .distinct()
                .limit(4)
                .toList();
        if (specialties.isEmpty() && artist.getGenre() != null && !artist.getGenre().isBlank()) {
            specialties = List.of(artist.getGenre());
        }

        return ArtistBrowseResponse.builder()
                .id(artist.getId())
                .name(artist.getName())
                .username(artist.getUsername())
                .location(artist.getLocation())
                .genre(artist.getGenre())
                .bio(artist.getBio())
                .experience(artist.getExperience())
                .profileImage(resolveImageUrl(artist.getProfileImage()))
                .profileImageLarge(resolveImageUrl(artist.getProfileImageLarge()))
                .profileImageMedium(resolveImageUrl(artist.getProfileImageMedium()))
                .profileImageThumbnail(resolveImageUrl(artist.getProfileImageThumbnail()))
                .verified(artist.isAccountVerified())
                .verificationStatus(artist.getVerificationStatus())
                .availabilityStatus(artist.isAvailable() ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.UNAVAILABLE)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .reviewCount(reviewRepository.countByArtistId(artist.getId()))
                .followerCount(engagementRepository.countByTargetTypeAndTargetIdAndAction(
                        EngagementTargetType.USER, String.valueOf(artist.getId()), EngagementAction.FOLLOW))
                .releasedProjectCount(0)
                .popularityScore(popularityService.userScore(artist.getId()))
                .trendingScore(popularityService.userTrendingScore(artist.getId()))
                .featured(artist.isFeatured())
                .available(artist.isAvailable() && services.stream().anyMatch(service -> service.isActive()))
                .specialties(specialties)
                .services(services)
                .build();
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

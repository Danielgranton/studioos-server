package com.studioos.server.studio;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.booking.Booking;
import com.studioos.server.beatmarketplace.BeatRepository;
import com.studioos.server.search.event.StudioCreatedEvent;
import com.studioos.server.search.event.StudioDeletedEvent;
import com.studioos.server.search.event.StudioUpdatedEvent;
import com.studioos.server.shared.media.ResponsiveImageAsset;
import com.studioos.server.shared.media.ResponsiveImageProcessingService;
import com.studioos.server.auth.service.ProfileImageServiceClient;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.AvailabilityStatus;
import com.studioos.server.engagement.PopularityService;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.studio.dto.CreateStudioRequest;
import com.studioos.server.studio.dto.RateStudioRequest;
import com.studioos.server.studio.dto.StudioResponse;
import com.studioos.server.studio.dto.UpdateStudioRequest;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudioServiceImpl {

    private final StudioRepository studioRepository;
    private final StudioRatingRepository ratingRepository;
    private final BeatRepository beatRepository;
    private final BookingRepository bookingRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final com.studioos.server.payment.TransactionRepository transactionRepository;
    private final com.studioos.server.payment.WithdrawalRepository withdrawalRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ResponsiveImageProcessingService responsiveImageProcessingService;
    private final ProfileImageServiceClient profileImageServiceClient;
    private final PresignedUrlService presignedUrlService;
    private final StudioMediaService studioMediaService;
    private final PopularityService popularityService;

    @org.springframework.beans.factory.annotation.Value("${storage.s3.profile-url-expiry-seconds:3600}")
    private int profileUrlExpirySeconds;

    // ─── Create studio (PRODUCER only) ───
   @Transactional
        public StudioResponse createStudio(User currentUser, CreateStudioRequest request) {
            if (currentUser.getRole() != Role.PRODUCER && currentUser.getRole() != Role.SUPER_ADMIN) {
                throw StudioosException.forbidden("Only producers can create studios");
            }

            // ─── Save studio first to get the generated ID ───
            Studio studio = Studio.builder()
                    .studioName(request.getStudioName())
                    .location(currentUser.getLocation() != null && !currentUser.getLocation().isBlank()
                            ? currentUser.getLocation()
                            : request.getLocation())
                    .pricing(request.getPricing())
                    .availability(request.getAvailability())
                    .description(request.getDescription())
                    .badge(request.getBadge())
                    .genres(request.getGenres() != null ? request.getGenres() : List.of())
                    .equipment(request.getEquipment() != null ? request.getEquipment() : List.of())
                    .rooms(request.getRooms())
                    .yearsActive(request.getYearsActive())
                    .responseTime(request.getResponseTime())
                    .available(request.getAvailable() == null || request.getAvailable())
                    .nextAvailable(request.getNextAvailable())
                    .ownerId(currentUser.getId())
                    .build();

            studioRepository.save(studio); // ← ID generated here

            // ─── Now add services with the real studio ID ───
            if (request.getServices() != null && !request.getServices().isEmpty()) {
                List<StudioService> services = request.getServices().stream()
                        .map(name -> StudioService.builder()
                                .name(name)
                                .studioId(studio.getId()) // ← now has real ID
                                .studio(studio)
                                .build())
                        .collect(Collectors.toList());
                studio.getServices().addAll(services);
                studioRepository.save(studio);
            }

            log.info("Studio created: {} by user: {}", studio.getStudioName(), currentUser.getEmail());
            applyProfileImage(studio, request.getProfileImage());
            studioRepository.save(studio);
            applicationEventPublisher.publishEvent(new StudioCreatedEvent(studio.getId()));
            return toResponse(studio);
        }

    // ─── Update studio ───
    @Transactional
    public StudioResponse updateStudio(User currentUser, String studioId, UpdateStudioRequest request) {
        Studio studio = findStudioAndVerifyOwner(studioId, currentUser);

        if (request.getStudioName() != null) studio.setStudioName(request.getStudioName());
        if (studio.getOwner() != null && studio.getOwner().getLocation() != null) {
            studio.setLocation(studio.getOwner().getLocation());
        }
        if (request.getPricing() != null) studio.setPricing(request.getPricing());
        if (request.getAvailability() != null) studio.setAvailability(request.getAvailability());
        if (request.getDescription() != null) studio.setDescription(request.getDescription());
        if (request.getBadge() != null) studio.setBadge(request.getBadge());
        if (request.getGenres() != null) studio.setGenres(request.getGenres());
        if (request.getEquipment() != null) studio.setEquipment(request.getEquipment());
        if (request.getRooms() != null) studio.setRooms(request.getRooms());
        if (request.getYearsActive() != null) studio.setYearsActive(request.getYearsActive());
        if (request.getResponseTime() != null) studio.setResponseTime(request.getResponseTime());
        if (request.getAvailable() != null) studio.setAvailable(request.getAvailable());
        if (request.getNextAvailable() != null) studio.setNextAvailable(request.getNextAvailable());
        if (request.getProfileImage() != null) applyProfileImage(studio, request.getProfileImage());

        // ─── Replace services if provided ───
        if (request.getServices() != null) {
            studio.getServices().clear();
            List<StudioService> services = request.getServices().stream()
                    .map(name -> StudioService.builder()
                            .name(name)
                            .studioId(studio.getId())
                            .studio(studio)
                            .build())
                    .collect(Collectors.toList());
            studio.getServices().addAll(services);
        }

        studioRepository.save(studio);
        log.info("Studio updated: {}", studioId);
        applicationEventPublisher.publishEvent(new StudioUpdatedEvent(studio.getId()));
        return toResponse(studio);
    }

    // ─── Upload studio image ───
    @Transactional
    public StudioResponse updateStudioImage(User currentUser, String studioId, MultipartFile file) {
        if (currentUser == null) {
            throw StudioosException.unauthorized("Authentication required");
        }
        if (file == null || file.isEmpty()) {
            throw StudioosException.badRequest("Studio image is required");
        }
        if (file.getSize() > 5L * 1024L * 1024L) {
            throw StudioosException.badRequest("Studio image must not exceed 5 MB");
        }

        String contentType = file.getContentType();
        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)
                && !"image/webp".equals(contentType)) {
            throw StudioosException.badRequest("Only JPEG, PNG, and WebP images are supported");
        }

        Studio studio = findStudioAndVerifyOwner(studioId, currentUser);
        try (var input = file.getInputStream()) {
            ResponsiveImageAsset image = profileImageServiceClient.processUploadedProfileImage(
                    input,
                    file.getSize(),
                    file.getOriginalFilename(),
                    contentType,
                    "studios/" + studio.getId() + "/profile",
                    "studio-" + studio.getId());
            applyImage(studio, image);
            studioRepository.save(studio);
            applicationEventPublisher.publishEvent(new StudioUpdatedEvent(studio.getId()));
            return toResponse(studio);
        } catch (java.io.IOException e) {
            throw StudioosException.badRequest("Could not read studio image");
        }
    }

    // ─── Delete studio ───
    @Transactional
    public void deleteStudio(User currentUser, String studioId) {
        Studio studio = findStudioAndVerifyOwner(studioId, currentUser);
        ensureStudioCanBeDeleted(studioId);
        studioRepository.delete(studio);
        log.info("Studio deleted: {}", studioId);
        applicationEventPublisher.publishEvent(new StudioDeletedEvent(studioId));
    }

    // ─── Get single studio ───
    public StudioResponse getStudio(String studioId) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));
        return toResponse(studio);
    }

    // ─── Get all studios (paginated + filters) ───
    public PageResponse<StudioResponse> getAllStudios(
            String location, Integer maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Studio> spec = StudioSpecifications.locationContains(location)
                .and(StudioSpecifications.pricingAtMost(maxPrice));

        Page<Studio> studios = studioRepository.findAll(spec, pageable);

        return PageResponse.from(
                studios.map(this::toResponse)
        );
    }

    public PageResponse<StudioResponse> getFeaturedStudios(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Studio> studios = studioRepository.findFeatured(pageable);

        return PageResponse.from(studios.map(this::toResponse));
    }

    public PageResponse<StudioResponse> getFeaturedStudios(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedFilter = filter == null ? "top-rated" : filter.trim().toLowerCase();
        Page<Studio> studios = switch (normalizedFilter) {
            case "available" -> studioRepository.findByOwnerAvailableTrue(pageable);
            case "most-booked" -> studioRepository.findAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookings")));
            case "premium" -> studioRepository.findByPricingGreaterThanEqual(3000, pageable);
            case "affordable" -> studioRepository.findByPricingLessThanEqual(2000, pageable);
            case "recording" -> studioRepository.findByService("record", pageable);
            case "mixing-mastering" -> studioRepository.findByService("mix", pageable);
            case "podcast" -> studioRepository.findByService("podcast", pageable);
            case "top-rated", "all" -> studioRepository.findFeatured(pageable);
            default -> throw StudioosException.badRequest("Unsupported studio filter");
        };

        return PageResponse.from(studios.map(this::toResponse));
    }

    // ─── Get my studios ───
    public List<StudioResponse> getMyStudios(User currentUser) {
        return studioRepository.findByOwnerId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Rate a studio ───
    @Transactional
    public void rateStudio(User currentUser, String studioId, RateStudioRequest request) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));

        if (studio.getOwnerId().equals(currentUser.getId())) {
            throw StudioosException.badRequest("You cannot rate your own studio");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> StudioosException.notFound("Booking not found"));

        if (!booking.getArtistId().equals(currentUser.getId()) && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You cannot rate this studio");
        }

        if (!studio.getId().equals(booking.getStudioId())) {
            throw StudioosException.badRequest("This booking does not belong to the specified studio");
        }

        if (booking.getStatus() != BookingStatus.DELIVERED
                || booking.getPaymentStatus() != BookingPaymentStatus.PAID) {
            throw StudioosException.badRequest("Studio reviews are only allowed after a completed booking");
        }

        StudioRating rating = ratingRepository
                .findByStudioIdAndUserId(studioId, currentUser.getId())
                .orElse(StudioRating.builder()
                        .studioId(studioId)
                        .userId(currentUser.getId())
                        .bookingId(booking.getId())
                        .build());

        rating.setBookingId(booking.getId());
        rating.setRating(request.getRating());
        rating.setReview(request.getReview());
        ratingRepository.save(rating);
        log.info("Studio {} rated {} by user {}", studioId, request.getRating(), currentUser.getEmail());
    }

    // ─── Helpers ───
    private Studio findStudioAndVerifyOwner(String studioId, User currentUser) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));

        if (!studio.getOwnerId().equals(currentUser.getId())
                && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You do not own this studio");
        }
        return studio;
    }

    private void ensureStudioCanBeDeleted(String studioId) {
        if (beatRepository.existsByStudioId(studioId)) {
            throw StudioosException.badRequest("Studio has beats and cannot be deleted");
        }
        if (bookingRepository.existsByStudioId(studioId)) {
            throw StudioosException.badRequest("Studio has bookings and cannot be deleted");
        }
        if (adCampaignRepository.existsByStudioId(studioId)) {
            throw StudioosException.badRequest("Studio has ad campaigns and cannot be deleted");
        }
        if (transactionRepository.existsByStudioId(studioId)) {
            throw StudioosException.badRequest("Studio has transactions and cannot be deleted");
        }
        if (withdrawalRepository.existsByStudioId(studioId)) {
            throw StudioosException.badRequest("Studio has withdrawals and cannot be deleted");
        }
    }

    private StudioResponse toResponse(Studio studio) {
        Double avgRating = ratingRepository.findAverageRatingByStudioId(studio.getId());
        Long totalRatings = ratingRepository.countByStudioId(studio.getId());

        return StudioResponse.builder()
                .id(studio.getId())
                .studioName(studio.getStudioName())
                .location(studio.getOwner() != null && studio.getOwner().getLocation() != null
                        ? studio.getOwner().getLocation()
                        : studio.getLocation())
                .pricing(studio.getPricing())
                .availability(studio.getAvailability())
                .description(studio.getDescription())
                .badge(studio.getBadge())
                .genres(studio.getGenres())
                .equipment(studio.getEquipment())
                .rooms(studio.getRooms())
                .yearsActive(studio.getYearsActive())
                .responseTime(studio.getResponseTime())
                .available(studio.getOwner() == null || studio.getOwner().isAvailable())
                .nextAvailable(studio.getNextAvailable())
                .bookings(studio.getBookings())
                .verified(studio.isVerified())
                .verificationStatus(studio.getVerificationStatus())
                .availabilityStatus(studio.getOwner() != null
                        ? (studio.getOwner().isAvailable() ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.UNAVAILABLE)
                        : (studio.isAvailable() ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.UNAVAILABLE))
                .profileImage(resolveImageUrl(studio.getProfileImage()))
                .profileImageLarge(resolveImageUrl(studio.getProfileImageLarge()))
                .profileImageMedium(resolveImageUrl(studio.getProfileImageMedium()))
                .profileImageThumbnail(resolveImageUrl(studio.getProfileImageThumbnail()))
                .ownerId(studio.getOwnerId())
                .ownerName(studio.getOwner() != null ? studio.getOwner().getName() : null)
                .ownerProfileImageThumbnail(resolveOwnerThumbnail(studio.getOwner()))
                .services(studio.getServices().stream()
                        .map(s -> s.getName())
                        .collect(Collectors.toList()))
                .media(studioMediaService.getMedia(studio.getId()))
                .averageRating(avgRating)
                .totalRatings(totalRatings)
                .popularityScore(popularityService.studioScore(studio.getId()))
                .trendingScore(popularityService.studioTrendingScore(studio.getId()))
                .featured(studio.isFeatured())
                .createdAt(studio.getCreatedAt())
                .build();
    }

    private String resolveOwnerThumbnail(User owner) {
        if (owner == null || owner.getProfileImageThumbnail() == null) return null;

        String reference = owner.getProfileImageThumbnail();
        if (reference.endsWith("/128.webp")) {
            reference = reference.substring(0, reference.length() - "/128.webp".length()) + "/64.webp";
        }
        return resolveImageUrl(reference);
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

    private void applyProfileImage(Studio studio, String profileImageReference) {
        ResponsiveImageAsset image = responsiveImageProcessingService.process(
                profileImageReference,
                "studios/" + studio.getId() + "/profile");
        if (image == null) {
            return;
        }

        studio.setProfileImage(image.getOriginalUrl());
        studio.setProfileImageLarge(image.getLargeUrl());
        studio.setProfileImageMedium(image.getMediumUrl());
        studio.setProfileImageThumbnail(image.getThumbnailUrl());
    }

    private void applyImage(Studio studio, ResponsiveImageAsset image) {
        if (image == null) return;
        studio.setProfileImage(image.getOriginalUrl());
        studio.setProfileImageLarge(image.getLargeUrl());
        studio.setProfileImageMedium(image.getMediumUrl());
        studio.setProfileImageThumbnail(image.getThumbnailUrl());
    }
}

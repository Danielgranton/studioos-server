package com.studioos.server.studio;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
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

    // ─── Create studio (PRODUCER only) ───
   @Transactional
        public StudioResponse createStudio(User currentUser, CreateStudioRequest request) {
            if (currentUser.getRole() != Role.PRODUCER && currentUser.getRole() != Role.SUPER_ADMIN) {
                throw StudioosException.forbidden("Only producers can create studios");
            }

            // ─── Save studio first to get the generated ID ───
            Studio studio = Studio.builder()
                    .studioName(request.getStudioName())
                    .location(request.getLocation())
                    .pricing(request.getPricing())
                    .availability(request.getAvailability())
                    .description(request.getDescription())
                    .profileImage(request.getProfileImage())
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
            return toResponse(studio);
        }

    // ─── Update studio ───
    @Transactional
    public StudioResponse updateStudio(User currentUser, String studioId, UpdateStudioRequest request) {
        Studio studio = findStudioAndVerifyOwner(studioId, currentUser);

        if (request.getStudioName() != null) studio.setStudioName(request.getStudioName());
        if (request.getLocation() != null) studio.setLocation(request.getLocation());
        if (request.getPricing() != null) studio.setPricing(request.getPricing());
        if (request.getAvailability() != null) studio.setAvailability(request.getAvailability());
        if (request.getDescription() != null) studio.setDescription(request.getDescription());
        if (request.getProfileImage() != null) studio.setProfileImage(request.getProfileImage());

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
        return toResponse(studio);
    }

    // ─── Delete studio ───
    @Transactional
    public void deleteStudio(User currentUser, String studioId) {
        Studio studio = findStudioAndVerifyOwner(studioId, currentUser);
        studioRepository.delete(studio);
        log.info("Studio deleted: {}", studioId);
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
        
        Page<Studio> studios = studioRepository.findAll(pageable);
        
        // Filter in memory
        List<StudioResponse> filtered = studios.stream()
                .filter(s -> location == null || s.getLocation().toLowerCase().contains(location.toLowerCase()))
                .filter(s -> maxPrice == null || s.getPricing() <= maxPrice)
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return PageResponse.from(
                new org.springframework.data.domain.PageImpl<>(
                        filtered,
                        pageable,
                        studios.getTotalElements()
                )
        );
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

        StudioRating rating = ratingRepository
                .findByStudioIdAndUserId(studioId, currentUser.getId())
                .orElse(StudioRating.builder()
                        .studioId(studioId)
                        .userId(currentUser.getId())
                        .build());

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

    private StudioResponse toResponse(Studio studio) {
        Double avgRating = ratingRepository.findAverageRatingByStudioId(studio.getId());
        Long totalRatings = ratingRepository.countByStudioId(studio.getId());

        return StudioResponse.builder()
                .id(studio.getId())
                .studioName(studio.getStudioName())
                .location(studio.getLocation())
                .pricing(studio.getPricing())
                .availability(studio.getAvailability())
                .description(studio.getDescription())
                .profileImage(studio.getProfileImage())
                .ownerId(studio.getOwnerId())
                .ownerName(studio.getOwner() != null ? studio.getOwner().getName() : null)
                .services(studio.getServices().stream()
                        .map(StudioService::getName)
                        .collect(Collectors.toList()))
                .averageRating(avgRating)
                .totalRatings(totalRatings)
                .createdAt(studio.getCreatedAt())
                .build();
    }
}
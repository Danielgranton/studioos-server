package com.studioos.server.servicecatalog;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.servicecatalog.dto.CreateCustomServiceRequest;
import com.studioos.server.servicecatalog.dto.ServiceCatalogResponse;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.artist.ArtistServiceOfferingRepository;
import com.studioos.server.artist.ArtistServiceOffering;
import com.studioos.server.servicecatalog.dto.ServiceProviderResponse;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.UserRepository;
import com.studioos.server.shared.storage.PresignedUrlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {
    private final ServiceCatalogRepository repository;
    private final ArtistServiceOfferingRepository artistOfferingRepository;
    private final StudioRepository studioRepository;
    private final UserRepository userRepository;
    private final PresignedUrlService presignedUrlService;

    @Value("${storage.s3.profile-url-expiry-seconds:3600}")
    private int profileUrlExpirySeconds;

    @Transactional(readOnly = true)
    public java.util.Optional<ServiceCatalog> findByName(String name) {
        return repository.findByNameIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<ServiceProviderResponse> getProviders(String slug, String providerType) {
        ServiceCatalog service = repository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> StudioosException.notFound("Service not found in StudioOS catalog"));
        List<ServiceProviderResponse> providers = new java.util.ArrayList<>();
        if (providerType == null || "ARTIST".equalsIgnoreCase(providerType)) {
            List<ArtistServiceOffering> offerings = new java.util.ArrayList<>(
                    artistOfferingRepository.findByCatalogServiceIdAndActiveTrueOrderByCreatedAtDesc(service.getId()));
            offerings.addAll(artistOfferingRepository.findActiveByNormalizedNames(serviceNameAliases(service)));
            Set<String> seenOfferings = new java.util.HashSet<>();
            for (ArtistServiceOffering offering : offerings) {
                if (!seenOfferings.add(offering.getId())) continue;
                userRepository.findById(offering.getArtistId()).ifPresent(user -> providers.add(new ServiceProviderResponse(
                        "ARTIST", String.valueOf(user.getId()), user.getName(), user.getLocation(), profileImage(user),
                        offering.getName(), offering.getDescription(), offering.getPrice(), offering.getCurrency(),
                        user.getVerificationStatus() != null && user.getVerificationStatus().name().equals("VERIFIED"),
                        offering.getId(), null, offering.getCatalogServiceId())));
            }
        }
        if (providerType == null || "PRODUCER".equalsIgnoreCase(providerType) || "STUDIO".equalsIgnoreCase(providerType)) {
            for (Studio studio : studioRepository.findAvailableByCatalogServiceIdOrName(service.getId(), serviceNameAliases(service))) {
                userRepository.findById(studio.getOwnerId()).ifPresent(producer -> providers.add(new ServiceProviderResponse(
                        "PRODUCER", String.valueOf(producer.getId()), producer.getName(), producer.getLocation(), profileImage(producer),
                        service.getName(), studio.getDescription(), studio.getPricing(), "KES",
                        studio.isVerified() || producer.getVerificationStatus() != null && producer.getVerificationStatus().name().equals("VERIFIED"),
                        studio.getId(), studio.getId(), service.getId())));
            }
        }
        return providers;
    }

    private List<String> serviceNameAliases(ServiceCatalog service) {
        Set<String> aliases = new java.util.LinkedHashSet<>();
        aliases.add(service.getName().trim().toLowerCase(Locale.ROOT));
        aliases.add(service.getSlug().replace('-', ' ').trim().toLowerCase(Locale.ROOT));
        if ("songwriting".equals(service.getSlug())) {
            aliases.addAll(List.of("song writing", "song writer", "songwriter", "lyric writing", "lyrics writing", "song composition"));
        } else if ("music-collaboration".equals(service.getSlug())) {
            aliases.addAll(List.of("collaboration", "collaborate on music", "musical collaboration", "music collab", "collab"));
        }
        return List.copyOf(aliases);
    }

    private String profileImage(User user) {
        if (user.getProfileImageThumbnail() != null && !user.getProfileImageThumbnail().isBlank()) {
            return resolveImageUrl(user.getProfileImageThumbnail());
        }
        if (user.getProfileImageMedium() != null && !user.getProfileImageMedium().isBlank()) {
            return resolveImageUrl(user.getProfileImageMedium());
        }
        return resolveImageUrl(user.getProfileImage());
    }

    private String resolveImageUrl(String reference) {
        if (reference == null || !reference.startsWith("s3://")) return reference;
        String remainder = reference.substring("s3://".length());
        int separator = remainder.indexOf('/');
        if (separator <= 0 || separator == remainder.length() - 1) return reference;
        try {
            return presignedUrlService.generateDownloadUrl(
                    remainder.substring(0, separator),
                    remainder.substring(separator + 1),
                    profileUrlExpirySeconds);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> getAvailable(Role role) {
        List<ServiceCatalog> services = role == Role.ARTIST
                ? repository.findByActiveTrueAndArtistAllowedTrueOrderByCategoryAscNameAsc()
                : role == Role.PRODUCER || role == Role.SUPER_ADMIN
                    ? repository.findByActiveTrueAndStudioAllowedTrueOrderByCategoryAscNameAsc()
                    : repository.findByActiveTrueOrderByCategoryAscNameAsc();
        return services.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceCatalog getActive(String id) {
        return repository.findById(id)
                .filter(ServiceCatalog::isActive)
                .orElseThrow(() -> StudioosException.notFound("Service not found in StudioOS catalog"));
    }

    @Transactional
    public ServiceCatalogResponse createCustom(User user, CreateCustomServiceRequest request) {
        String name = request.name().trim();
        ServiceCatalog existing = repository.findByNameIgnoreCase(name).orElse(null);
        if (existing != null) return toResponse(existing);

        ServiceCatalog service = ServiceCatalog.builder()
                .name(name)
                .slug(uniqueSlug(name))
                .category(request.category().trim())
                .description(request.description() == null ? null : request.description().trim())
                .artistAllowed(user.getRole() == Role.ARTIST || user.getRole() == Role.SUPER_ADMIN)
                .studioAllowed(user.getRole() == Role.PRODUCER || user.getRole() == Role.SUPER_ADMIN)
                .createdBy(user.getId())
                .build();
        return toResponse(repository.save(service));
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        String slug = base.isBlank() ? "custom-service" : base;
        int suffix = 2;
        while (repository.findBySlugAndActiveTrue(slug).isPresent()) slug = base + "-" + suffix++;
        return slug;
    }

    private ServiceCatalogResponse toResponse(ServiceCatalog service) {
        return new ServiceCatalogResponse(service.getId(), service.getSlug(), service.getName(), service.getCategory(), service.getDescription(), service.isArtistAllowed(), service.isStudioAllowed());
    }
}

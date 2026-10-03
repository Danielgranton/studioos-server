package com.studioos.server.servicecatalog;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {
    private final ServiceCatalogRepository repository;
    private final ArtistServiceOfferingRepository artistOfferingRepository;
    private final StudioRepository studioRepository;
    private final UserRepository userRepository;

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
            for (ArtistServiceOffering offering : artistOfferingRepository.findByCatalogServiceIdAndActiveTrueOrderByCreatedAtDesc(service.getId())) {
                userRepository.findById(offering.getArtistId()).ifPresent(user -> providers.add(new ServiceProviderResponse(
                        "ARTIST", String.valueOf(user.getId()), user.getName(), user.getLocation(), user.getProfileImageThumbnail(),
                        offering.getName(), offering.getDescription(), offering.getPrice(), offering.getCurrency(), user.getVerificationStatus() != null && user.getVerificationStatus().name().equals("VERIFIED"))));
            }
        }
        if (providerType == null || "STUDIO".equalsIgnoreCase(providerType)) {
            for (Studio studio : studioRepository.findAvailableByCatalogServiceId(service.getId())) {
                providers.add(new ServiceProviderResponse(
                        "STUDIO", studio.getId(), studio.getStudioName(), studio.getLocation(), studio.getProfileImageThumbnail(),
                        service.getName(), studio.getDescription(), studio.getPricing(), "KES", studio.isVerified()));
            }
        }
        return providers;
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

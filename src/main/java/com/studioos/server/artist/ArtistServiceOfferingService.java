package com.studioos.server.artist;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.artist.dto.ArtistServiceOfferingRequest;
import com.studioos.server.artist.dto.ArtistServiceOfferingResponse;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistServiceOfferingService {

    private final ArtistServiceOfferingRepository offeringRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ArtistServiceOfferingResponse> getPublicServices(Integer artistId) {
        return offeringRepository.findByArtistIdAndActiveTrueOrderByCreatedAtDesc(artistId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ArtistServiceOfferingResponse> getMyServices(User currentUser) {
        requireArtist(currentUser);
        return offeringRepository.findByArtistIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ArtistServiceOfferingResponse create(User currentUser, ArtistServiceOfferingRequest request) {
        requireArtist(currentUser);
        ArtistServiceOffering offering = ArtistServiceOffering.builder()
                .artistId(currentUser.getId())
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .price(request.getPrice())
                .currency(normalizeCurrency(request.getCurrency()))
                .active(request.getActive() == null || request.getActive())
                .build();
        return toResponse(offeringRepository.save(offering));
    }

    @Transactional
    public ArtistServiceOfferingResponse update(User currentUser, String offeringId, ArtistServiceOfferingRequest request) {
        requireArtist(currentUser);
        ArtistServiceOffering offering = offeringRepository.findByIdAndArtistId(offeringId, currentUser.getId())
                .orElseThrow(() -> StudioosException.notFound("Artist service not found"));

        offering.setName(request.getName().trim());
        offering.setDescription(trimToNull(request.getDescription()));
        offering.setPrice(request.getPrice());
        offering.setCurrency(normalizeCurrency(request.getCurrency()));
        offering.setActive(request.getActive() == null || request.getActive());
        return toResponse(offeringRepository.save(offering));
    }

    @Transactional
    public void delete(User currentUser, String offeringId) {
        requireArtist(currentUser);
        ArtistServiceOffering offering = offeringRepository.findByIdAndArtistId(offeringId, currentUser.getId())
                .orElseThrow(() -> StudioosException.notFound("Artist service not found"));
        offeringRepository.delete(offering);
    }

    private void requireArtist(User user) {
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> StudioosException.notFound("User not found"));
        if (currentUser.getRole() != Role.ARTIST) {
            throw StudioosException.forbidden("Only artists can manage artist services");
        }
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "KES" : currency.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private ArtistServiceOfferingResponse toResponse(ArtistServiceOffering offering) {
        return ArtistServiceOfferingResponse.builder()
                .id(offering.getId())
                .artistId(offering.getArtistId())
                .name(offering.getName())
                .description(offering.getDescription())
                .price(offering.getPrice())
                .currency(offering.getCurrency())
                .active(offering.isActive())
                .createdAt(offering.getCreatedAt())
                .updatedAt(offering.getUpdatedAt())
                .build();
    }
}

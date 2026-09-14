package com.studioos.server.artist;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.artist.dto.ArtistServiceRequestResponse;
import com.studioos.server.artist.dto.CreateArtistServiceRequest;
import com.studioos.server.artist.dto.UpdateArtistServiceRequest;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistServiceRequestService {
    private final ArtistServiceRequestRepository requestRepository;
    private final ArtistServiceOfferingRepository offeringRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ArtistServiceRequestResponse> getMyRequests(User currentUser) {
        requireArtist(currentUser);
        return requestRepository.findByArtistIdOrderByCreatedAtDesc(currentUser.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ArtistServiceRequestResponse create(User currentUser, Integer artistId, String serviceId, CreateArtistServiceRequest request) {
        ArtistServiceOffering offering = offeringRepository.findById(serviceId)
                .filter(item -> item.getArtistId().equals(artistId) && item.isActive())
                .orElseThrow(() -> StudioosException.notFound("Artist service not found"));
        if (currentUser == null || currentUser.getId() == null || currentUser.getId().equals(artistId)) {
            throw StudioosException.badRequest("A different account is required to request this service");
        }
        ArtistServiceRequest created = requestRepository.save(ArtistServiceRequest.builder()
                .serviceId(offering.getId())
                .artistId(artistId)
                .requesterId(currentUser.getId())
                .requestNote(request == null ? null : trimToNull(request.getRequestNote()))
                .amount(offering.getPrice())
                .currency(offering.getCurrency())
                .build());
        return toResponse(created);
    }

    @Transactional
    public ArtistServiceRequestResponse update(User currentUser, String requestId, UpdateArtistServiceRequest request) {
        requireArtist(currentUser);
        ArtistServiceRequest serviceRequest = requestRepository.findByIdAndArtistId(requestId, currentUser.getId())
                .orElseThrow(() -> StudioosException.notFound("Service request not found"));
        validateTransition(serviceRequest.getStatus(), request.getStatus());
        serviceRequest.setStatus(request.getStatus());
        return toResponse(requestRepository.save(serviceRequest));
    }

    private void validateTransition(ArtistServiceRequestStatus current, ArtistServiceRequestStatus next) {
        if (current == ArtistServiceRequestStatus.PENDING && (next == ArtistServiceRequestStatus.PAID || next == ArtistServiceRequestStatus.CANCELLED)) return;
        if (current == ArtistServiceRequestStatus.PAID && (next == ArtistServiceRequestStatus.DELIVERED || next == ArtistServiceRequestStatus.CANCELLED)) return;
        if (current == next) return;
        throw StudioosException.badRequest("Invalid service request status transition");
    }

    private void requireArtist(User user) {
        User currentUser = userRepository.findById(user.getId()).orElseThrow(() -> StudioosException.notFound("User not found"));
        if (currentUser.getRole() != Role.ARTIST) throw StudioosException.forbidden("Only artists can manage service requests");
    }

    private ArtistServiceRequestResponse toResponse(ArtistServiceRequest request) {
        ArtistServiceOffering offering = offeringRepository.findById(request.getServiceId()).orElse(null);
        User requester = userRepository.findById(request.getRequesterId()).orElse(null);
        return ArtistServiceRequestResponse.builder()
                .id(request.getId()).serviceId(request.getServiceId()).serviceName(offering == null ? "Service" : offering.getName())
                .requesterId(request.getRequesterId()).requesterName(requester == null ? "StudioOS member" : requester.getName())
                .requestNote(request.getRequestNote()).amount(request.getAmount()).currency(request.getCurrency()).status(request.getStatus())
                .createdAt(request.getCreatedAt()).updatedAt(request.getUpdatedAt()).build();
    }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

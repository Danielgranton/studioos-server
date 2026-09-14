package com.studioos.server.artist;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.artist.dto.ArtistServiceOfferingRequest;
import com.studioos.server.artist.dto.ArtistServiceOfferingResponse;
import com.studioos.server.artist.dto.ArtistServiceRequestResponse;
import com.studioos.server.artist.dto.CreateArtistServiceRequest;
import com.studioos.server.artist.dto.UpdateArtistServiceRequest;
import com.studioos.server.artist.dto.ArtistBrowseResponse;
import com.studioos.server.artist.dto.ArtistReviewResponse;
import com.studioos.server.artist.dto.RateArtistRequest;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
@Validated
public class ArtistServiceOfferingController {

    private final ArtistServiceOfferingService offeringService;
    private final ArtistServiceRequestService requestService;
    private final ArtistBrowseService browseService;
    private final ArtistReviewService reviewService;

    @PostMapping("/{artistId}/reviews")
    public ResponseEntity<ApiResponse<ArtistReviewResponse>> submitReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer artistId,
            @Valid @RequestBody RateArtistRequest request
    ) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(reviewService.submitReview(currentUser, artistId, request)));
    }

    @GetMapping("/{artistId}/reviews")
    public ResponseEntity<ApiResponse<Page<ArtistReviewResponse>>> getReviews(
            @PathVariable Integer artistId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviews(artistId, pageable)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ArtistBrowseResponse>>> getArtists(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(browseService.getArtists(page, size)));
    }

    @GetMapping("/{artistId}/services")
    public ResponseEntity<ApiResponse<List<ArtistServiceOfferingResponse>>> getPublicServices(
            @PathVariable Integer artistId
    ) {
        return ResponseEntity.ok(ApiResponse.success(offeringService.getPublicServices(artistId)));
    }

    @GetMapping("/me/services")
    public ResponseEntity<ApiResponse<List<ArtistServiceOfferingResponse>>> getMyServices(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(offeringService.getMyServices(currentUser)));
    }

    @PostMapping("/me/services")
    public ResponseEntity<ApiResponse<ArtistServiceOfferingResponse>> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ArtistServiceOfferingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Artist service created", offeringService.create(currentUser, request)));
    }

    @PutMapping("/me/services/{serviceId}")
    public ResponseEntity<ApiResponse<ArtistServiceOfferingResponse>> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String serviceId,
            @Valid @RequestBody ArtistServiceOfferingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Artist service updated", offeringService.update(currentUser, serviceId, request)));
    }

    @DeleteMapping("/me/services/{serviceId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String serviceId
    ) {
        offeringService.delete(currentUser, serviceId);
        return ResponseEntity.ok(ApiResponse.success("Artist service deleted"));
    }

    @GetMapping("/me/service-requests")
    public ResponseEntity<ApiResponse<List<ArtistServiceRequestResponse>>> getMyRequests(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getMyRequests(currentUser)));
    }

    @PostMapping("/{artistId}/services/{serviceId}/requests")
    public ResponseEntity<ApiResponse<ArtistServiceRequestResponse>> createRequest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer artistId,
            @PathVariable String serviceId,
            @Valid @RequestBody CreateArtistServiceRequest request
    ) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success("Service request created", requestService.create(currentUser, artistId, serviceId, request)));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/me/service-requests/{requestId}")
    public ResponseEntity<ApiResponse<ArtistServiceRequestResponse>> updateRequest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String requestId,
            @Valid @RequestBody UpdateArtistServiceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Service request updated", requestService.update(currentUser, requestId, request)));
    }
}

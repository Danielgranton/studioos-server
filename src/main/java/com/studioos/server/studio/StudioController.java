package com.studioos.server.studio;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.studio.dto.*;
import com.studioos.server.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/studios")
@RequiredArgsConstructor
public class StudioController {

    private final StudioServiceImpl studioService;
    private final StudioMediaService studioMediaService;
    private final StudioReviewService studioReviewService;

    // ─── Create studio ───
    @PostMapping
    public ResponseEntity<ApiResponse<StudioResponse>> createStudio(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateStudioRequest request
    ) {
        StudioResponse response = studioService.createStudio(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Studio created successfully", response));
    }

    // ─── Update studio ───
    @PutMapping("/{studioId}")
    public ResponseEntity<ApiResponse<StudioResponse>> updateStudio(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId,
            @RequestBody UpdateStudioRequest request
    ) {
        StudioResponse response = studioService.updateStudio(currentUser, studioId, request);
        return ResponseEntity.ok(ApiResponse.success("Studio updated successfully", response));
    }

    @PostMapping(value = "/{studioId}/image", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<StudioResponse>> updateStudioImage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId,
            @RequestPart("file") MultipartFile file
    ) {
        StudioResponse response = studioService.updateStudioImage(currentUser, studioId, file);
        return ResponseEntity.ok(ApiResponse.success("Studio image updated successfully", response));
    }

    @PostMapping(value = "/{studioId}/media/images", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<StudioMediaResponse>> uploadGalleryImage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Studio gallery image uploaded successfully",
                studioMediaService.uploadImage(currentUser, studioId, file)));
    }

    @PostMapping("/{studioId}/media/video/upload")
    public ResponseEntity<ApiResponse<StudioVideoUploadResponse>> createVideoUpload(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId,
            @RequestParam String contentType,
            @RequestParam long contentLength
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                studioMediaService.createVideoUpload(currentUser, studioId, contentType, contentLength)));
    }

    @PostMapping("/{studioId}/media/video/{mediaId}/complete")
    public ResponseEntity<ApiResponse<StudioMediaResponse>> completeVideoUpload(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId,
            @PathVariable String mediaId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Studio video uploaded successfully",
                studioMediaService.completeVideoUpload(currentUser, studioId, mediaId)));
    }

    @DeleteMapping("/{studioId}/media/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudioMedia(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId,
            @PathVariable String mediaId
    ) {
        studioMediaService.deleteMedia(currentUser, studioId, mediaId);
        return ResponseEntity.ok(ApiResponse.success("Studio media deleted successfully"));
    }

    // ─── Delete studio ───
    @DeleteMapping("/{studioId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudio(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId
    ) {
        studioService.deleteStudio(currentUser, studioId);
        return ResponseEntity.ok(ApiResponse.success("Studio deleted successfully"));
    }

    // ─── Get single studio ───
    @GetMapping("/{studioId}")
    public ResponseEntity<ApiResponse<StudioResponse>> getStudio(@PathVariable String studioId) {
        StudioResponse response = studioService.getStudio(studioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{studioId}/reviews")
    public ResponseEntity<ApiResponse<Page<StudioReviewResponse>>> getReviews(
            @PathVariable String studioId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(studioReviewService.getReviews(studioId, pageable)));
    }

    // ─── Get all studios (public, paginated) ───
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<PageResponse<StudioResponse>>> getFeaturedStudios(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<StudioResponse> response = studioService.getFeaturedStudios(filter, page, Math.min(size, 10));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StudioResponse>>> getAllStudios(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<StudioResponse> response = studioService.getAllStudios(location, maxPrice, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Get my studios ───
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<StudioResponse>>> getMyStudios(
            @AuthenticationPrincipal User currentUser
    ) {
        List<StudioResponse> response = studioService.getMyStudios(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Rate a studio ───
    @PostMapping("/{studioId}/rate")
    public ResponseEntity<ApiResponse<Void>> rateStudio(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId,
            @Valid @RequestBody RateStudioRequest request
    ) {
        studioService.rateStudio(currentUser, studioId, request);
        return ResponseEntity.ok(ApiResponse.success("Studio rated successfully"));
    }

    @GetMapping("/{studioId}/like")
    public ResponseEntity<ApiResponse<StudioLikeStateResponse>> getLikeState(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId
    ) {
        return ResponseEntity.ok(ApiResponse.success(studioService.getLikeState(currentUser, studioId)));
    }

    @PostMapping("/{studioId}/like")
    public ResponseEntity<ApiResponse<StudioLikeStateResponse>> likeStudio(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId
    ) {
        return ResponseEntity.ok(ApiResponse.success(studioService.likeStudio(currentUser, studioId)));
    }

    @DeleteMapping("/{studioId}/like")
    public ResponseEntity<ApiResponse<StudioLikeStateResponse>> unlikeStudio(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId
    ) {
        return ResponseEntity.ok(ApiResponse.success(studioService.unlikeStudio(currentUser, studioId)));
    }
}

package com.studioos.server.studio;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.studio.dto.*;
import com.studioos.server.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/studios")
@RequiredArgsConstructor
public class StudioController {

    private final StudioServiceImpl studioService;

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

    // ─── Get all studios (public, paginated) ───
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
}
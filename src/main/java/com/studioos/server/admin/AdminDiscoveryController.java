package com.studioos.server.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.search.index.ProducerSearchIndexingService;
import com.studioos.server.search.index.StudioSearchIndexingService;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/discovery")
@RequiredArgsConstructor
@Validated
public class AdminDiscoveryController {

    private final UserRepository userRepository;
    private final StudioRepository studioRepository;
    private final ProducerSearchIndexingService producerSearchIndexingService;
    private final StudioSearchIndexingService studioSearchIndexingService;

    @PutMapping("/users/{userId}/featured")
    public ResponseEntity<ApiResponse<Void>> setUserFeatured(
            @PathVariable Integer userId,
            @Valid @RequestBody FeaturedFlagRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.studioos.server.shared.exceptions.StudioosException.notFound("User not found"));
        user.setFeatured(request.getFeatured());
        userRepository.save(user);
        if (user.getRole() == com.studioos.server.shared.enums.Role.PRODUCER) {
            producerSearchIndexingService.indexProducer(user);
        }
        return ResponseEntity.ok(ApiResponse.success("User featured status updated"));
    }

    @PutMapping("/studios/{studioId}/featured")
    public ResponseEntity<ApiResponse<Void>> setStudioFeatured(
            @PathVariable String studioId,
            @Valid @RequestBody FeaturedFlagRequest request
    ) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> com.studioos.server.shared.exceptions.StudioosException.notFound("Studio not found"));
        studio.setFeatured(request.getFeatured());
        studioRepository.save(studio);
        studioSearchIndexingService.indexStudio(studio);
        return ResponseEntity.ok(ApiResponse.success("Studio featured status updated"));
    }
}

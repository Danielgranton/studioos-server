package com.studioos.server.beatmarketplace;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.beatmarketplace.dto.BeatReviewResponse;
import com.studioos.server.beatmarketplace.dto.SubmitReviewRequest;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/beats")
@RequiredArgsConstructor
public class BeatReviewController {

    private final BeatReviewService beatReviewService;

    @PostMapping("/{beatId}/reviews")
    public BeatReviewResponse submitReview(
            @AuthenticationPrincipal User user,
            @PathVariable String beatId,
            @Valid @RequestBody SubmitReviewRequest request) {
        return beatReviewService.submitReview(user.getId(), beatId, request);
    }

    @GetMapping("/{beatId}/reviews")
    public List<BeatReviewResponse> getReviews(@PathVariable String beatId) {
        return beatReviewService.getReviewsForBeat(beatId);
    }

    @DeleteMapping("/{beatId}/reviews")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal User user,
            @PathVariable String beatId) {
        beatReviewService.deleteReview(user.getId(), beatId);
        return ResponseEntity.noContent().build();
    }
}
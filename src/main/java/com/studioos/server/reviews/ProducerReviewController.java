package com.studioos.server.reviews;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.reviews.dto.ProducerReviewResponse;
import com.studioos.server.reviews.dto.RateProducerRequest;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/producers")
@RequiredArgsConstructor
public class ProducerReviewController {

    private final ProducerReviewService producerReviewService;

    @PostMapping("/{producerId}/reviews")
    public ResponseEntity<ProducerReviewResponse> submitReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer producerId,
            @Valid @RequestBody RateProducerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(producerReviewService.submitReview(currentUser, producerId, request));
    }

    @GetMapping("/{producerId}/reviews")
    public List<ProducerReviewResponse> getReviews(@PathVariable Integer producerId) {
        return producerReviewService.getReviews(producerId);
    }
}

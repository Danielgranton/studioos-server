package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.dto.BeatReviewResponse;
import com.studioos.server.beatmarketplace.dto.SubmitReviewRequest;
import com.studioos.server.shared.enums.BeatPaymentStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeatReviewService {

    private final BeatRepository beatRepository;
    private final BeatReviewRepository beatReviewRepository;
    private final BeatPurchaseRepository beatPurchaseRepository;
    private final BeatDownloadRepository beatDownloadRepository;

    @Transactional
    public BeatReviewResponse submitReview(Integer userId, String beatId, SubmitReviewRequest request) {
        if (request.getPurchaseId() == null || request.getPurchaseId().isBlank()) {
            throw StudioosException.badRequest("Purchase ID is required");
        }

        beatRepository.findById(beatId)
                .orElseThrow(() -> new IllegalArgumentException("Beat not found: " + beatId));

        BeatPurchase purchase = beatPurchaseRepository.findById(request.getPurchaseId())
                .orElseThrow(() -> StudioosException.notFound("Purchase not found"));

        if (!purchase.getBeatId().equals(beatId) || !purchase.getBuyerId().equals(userId)) {
            throw new SecurityException("You cannot review this beat");
        }

        if (purchase.getStatus() != BeatPaymentStatus.PAID) {
            throw new SecurityException("Only verified buyers can review this beat");
        }

        boolean hasDownloaded = !beatDownloadRepository.findByPurchaseId(purchase.getId()).isEmpty();
        if (!hasDownloaded) {
            throw new SecurityException("You must download the beat before reviewing it");
        }

        BeatReview review = beatReviewRepository.findByUserIdAndBeatId(userId, beatId)
                .map(existing -> {
                    existing.setRating(request.getRating());
                    existing.setComment(request.getComment());
                    existing.setPurchaseId(purchase.getId());
                    return existing;
                })
                .orElseGet(() -> BeatReview.builder()
                        .userId(userId)
                        .beatId(beatId)
                        .purchaseId(purchase.getId())
                        .rating(request.getRating())
                        .comment(request.getComment())
                        .build());

        review = beatReviewRepository.save(review);

        return toResponse(review);
    }

    public List<BeatReviewResponse> getReviewsForBeat(String beatId) {
        return beatReviewRepository.findByBeatId(beatId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReview(Integer userId, String beatId) {
        BeatReview review = beatReviewRepository.findByUserIdAndBeatId(userId, beatId)
                .orElseThrow(() -> new IllegalArgumentException("You have not reviewed this beat"));

        beatReviewRepository.delete(review);
    }

    private BeatReviewResponse toResponse(BeatReview review) {
        return BeatReviewResponse.builder()
                .id(review.getId())
                .beatId(review.getBeatId())
                .userId(review.getUserId())
                .purchaseId(review.getPurchaseId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

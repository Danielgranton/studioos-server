package com.studioos.server.studio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.reviews.ReviewCommentRepository;
import com.studioos.server.reviews.ReviewReactionRepository;
import com.studioos.server.reviews.ReviewReactionType;
import com.studioos.server.reviews.ReviewType;
import com.studioos.server.reviews.ReviewModerationStatus;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.dto.StudioReviewResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudioReviewService {

    private final StudioRepository studioRepository;
    private final StudioRatingRepository ratingRepository;
    private final ReviewReactionRepository reactionRepository;
    private final ReviewCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public Page<StudioReviewResponse> getReviews(String studioId, Pageable pageable) {
        if (!studioRepository.existsById(studioId)) {
            throw StudioosException.notFound("Studio not found");
        }

        return ratingRepository.findByStudioIdAndModerationStatus(studioId, ReviewModerationStatus.ACTIVE, pageable).map(rating -> {
            var reviewer = rating.getUser();
            return StudioReviewResponse.builder()
                    .id(rating.getId())
                    .studioId(rating.getStudioId())
                    .reviewerId(rating.getUserId())
                    .reviewerName(reviewer != null ? reviewer.getName() : null)
                    .reviewerUsername(reviewer != null ? reviewer.getUsername() : null)
                    .reviewerRole(reviewer != null && reviewer.getRole() != null ? reviewer.getRole().name() : null)
                    .reviewerAvatar(reviewer != null ? reviewer.getProfileImageThumbnail() : null)
                    .bookingId(rating.getBookingId())
                    .rating(rating.getRating())
                    .review(rating.getReview())
                    .likes(reactionRepository.countByReviewTypeAndReviewIdAndReaction(
                            ReviewType.STUDIO, rating.getId(), ReviewReactionType.LIKE))
                    .comments(commentRepository.countByReviewTypeAndReviewIdAndDeletedAtIsNull(
                            ReviewType.STUDIO, rating.getId()))
                    .dislikes(reactionRepository.countByReviewTypeAndReviewIdAndReaction(
                            ReviewType.STUDIO, rating.getId(), ReviewReactionType.DISLIKE))
                    .createdAt(rating.getCreatedAt())
                    .build();
        });
    }
}

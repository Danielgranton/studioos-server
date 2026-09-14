package com.studioos.server.reviews;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.reviews.dto.ProducerReviewResponse;
import com.studioos.server.reviews.dto.RateProducerRequest;
import com.studioos.server.reviews.ReviewModerationStatus;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.Role;

import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProducerReviewService {

    private final BookingRepository bookingRepository;
    private final ProducerReviewRepository producerReviewRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final ReviewCommentRepository reviewCommentRepository;

    @Transactional
    public ProducerReviewResponse submitReview(User currentUser, Integer producerId, RateProducerRequest request) {
        if (currentUser.getRole() != Role.ARTIST && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Only artists can review producers");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> StudioosException.notFound("Booking not found"));

        if (!booking.getArtistId().equals(currentUser.getId()) && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You cannot review this producer");
        }

        if (!booking.getStatus().equals(BookingStatus.DELIVERED)
                || !booking.getPaymentStatus().equals(BookingPaymentStatus.PAID)) {
            throw StudioosException.badRequest("Producer reviews are only allowed after a completed booking");
        }

        if (booking.getStudio() == null || booking.getStudio().getOwnerId() == null
                || !booking.getStudio().getOwnerId().equals(producerId)) {
            throw StudioosException.badRequest("This booking does not belong to the specified producer");
        }

        ProducerReview review = producerReviewRepository.findByUserIdAndProducerId(currentUser.getId(), producerId)
                .orElse(ProducerReview.builder()
                        .userId(currentUser.getId())
                        .producerId(producerId)
                        .bookingId(booking.getId())
                        .build());

        review.setBookingId(booking.getId());
        review.setRating(request.getRating());
        review.setReview(request.getReview());
        review.setModerationStatus(ReviewModerationStatus.ACTIVE);
        review.setModeratedAt(null);
        review.setModeratedBy(null);
        review.setModerationReason(null);
        review = producerReviewRepository.save(review);

        return toResponse(review);
    }

    public Page<ProducerReviewResponse> getReviews(Integer producerId, Pageable pageable) {
        return producerReviewRepository.findByProducerIdAndModerationStatus(producerId, ReviewModerationStatus.ACTIVE, pageable).map(this::toResponse);
    }

    private ProducerReviewResponse toResponse(ProducerReview review) {
        return ProducerReviewResponse.builder()
                .id(review.getId())
                .producerId(review.getProducerId())
                .reviewerId(review.getUserId())
                .reviewerName(review.getReviewer() != null ? review.getReviewer().getName() : null)
                .reviewerUsername(review.getReviewer() != null ? review.getReviewer().getUsername() : null)
                .reviewerRole(review.getReviewer() != null && review.getReviewer().getRole() != null
                        ? review.getReviewer().getRole().name() : null)
                .reviewerAvatar(review.getReviewer() != null ? review.getReviewer().getProfileImageThumbnail() : null)
                .bookingId(review.getBookingId())
                .rating(review.getRating())
                .review(review.getReview())
                .likes(reviewReactionRepository.countByReviewTypeAndReviewIdAndReaction(
                        ReviewType.PRODUCER, review.getId(), ReviewReactionType.LIKE))
                .comments(reviewCommentRepository.countByReviewTypeAndReviewIdAndDeletedAtIsNull(ReviewType.PRODUCER, review.getId()))
                .dislikes(reviewReactionRepository.countByReviewTypeAndReviewIdAndReaction(
                        ReviewType.PRODUCER, review.getId(), ReviewReactionType.DISLIKE))
                .createdAt(review.getCreatedAt())
                .build();
    }
}

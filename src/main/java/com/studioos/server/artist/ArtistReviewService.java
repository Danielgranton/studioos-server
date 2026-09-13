package com.studioos.server.artist;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.artist.dto.ArtistReviewResponse;
import com.studioos.server.artist.dto.RateArtistRequest;
import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistReviewService {

    private final BookingRepository bookingRepository;
    private final ArtistReviewRepository artistReviewRepository;

    @Transactional
    public ArtistReviewResponse submitReview(User currentUser, Integer artistId, RateArtistRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> StudioosException.notFound("Booking not found"));

        if (currentUser.getRole() != Role.SUPER_ADMIN
                && (currentUser.getRole() != Role.PRODUCER || booking.getStudio() == null
                || !booking.getStudio().getOwnerId().equals(currentUser.getId()))) {
            throw StudioosException.forbidden("Only the booked studio owner can review this artist");
        }
        if (!booking.getArtistId().equals(artistId)) {
            throw StudioosException.badRequest("This booking does not belong to the specified artist");
        }
        if (booking.getStatus() != BookingStatus.DELIVERED
                || booking.getPaymentStatus() != BookingPaymentStatus.PAID) {
            throw StudioosException.badRequest("Artist reviews are only allowed after a completed booking");
        }

        Integer reviewerId = currentUser.getId();
        ArtistReview review = artistReviewRepository.findByReviewerIdAndArtistId(reviewerId, artistId)
                .orElse(ArtistReview.builder()
                        .reviewerId(reviewerId)
                        .artistId(artistId)
                        .bookingId(booking.getId())
                        .build());
        review.setBookingId(booking.getId());
        review.setRating(request.getRating());
        review.setReview(request.getReview());
        return toResponse(artistReviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ArtistReviewResponse> getReviews(Integer artistId) {
        return artistReviewRepository.findByArtistId(artistId).stream().map(this::toResponse).toList();
    }

    private ArtistReviewResponse toResponse(ArtistReview review) {
        return ArtistReviewResponse.builder()
                .id(review.getId())
                .artistId(review.getArtistId())
                .reviewerId(review.getReviewerId())
                .bookingId(review.getBookingId())
                .rating(review.getRating())
                .review(review.getReview())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

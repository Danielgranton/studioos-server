package com.studioos.server.reviews;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.reviews.dto.ProducerReviewResponse;
import com.studioos.server.reviews.dto.RateProducerRequest;
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
        review = producerReviewRepository.save(review);

        return toResponse(review);
    }

    public List<ProducerReviewResponse> getReviews(Integer producerId) {
        return producerReviewRepository.findByProducerId(producerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProducerReviewResponse toResponse(ProducerReview review) {
        return ProducerReviewResponse.builder()
                .id(review.getId())
                .producerId(review.getProducerId())
                .reviewerId(review.getUserId())
                .bookingId(review.getBookingId())
                .rating(review.getRating())
                .review(review.getReview())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

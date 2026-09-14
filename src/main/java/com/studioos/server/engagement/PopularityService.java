package com.studioos.server.engagement;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

import com.studioos.server.booking.BookingRepository;
import com.studioos.server.artist.ArtistReviewRepository;
import com.studioos.server.reviews.ProducerReviewRepository;
import com.studioos.server.reviews.ReviewModerationStatus;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.studio.StudioRatingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopularityService {

    private final EngagementEdgeRepository engagementRepository;
    private final EngagementViewRepository viewRepository;
    private final BookingRepository bookingRepository;
    private final ArtistReviewRepository artistReviewRepository;
    private final ProducerReviewRepository producerReviewRepository;
    private final StudioRatingRepository studioRatingRepository;

    public double userScore(Integer userId) {
        long followers = count(EngagementTargetType.USER, userId, EngagementAction.FOLLOW);
        long favorites = count(EngagementTargetType.USER, userId, EngagementAction.FAVORITE);
        long views = viewRepository.countByTargetTypeAndTargetId(EngagementTargetType.USER, String.valueOf(userId));
        long completed = bookingRepository.countByArtistIdAndStatusAndPaymentStatus(userId, BookingStatus.DELIVERED, BookingPaymentStatus.PAID)
                + bookingRepository.countByProducerIdAndStatusAndPaymentStatus(userId, BookingStatus.DELIVERED, BookingPaymentStatus.PAID);
        double rating = average(artistReviewRepository.findAverageRatingByArtistId(userId), producerReviewRepository.findAverageRatingByProducerId(userId));
        long reviews = artistReviewRepository.countByArtistIdAndModerationStatus(userId, ReviewModerationStatus.ACTIVE)
                + producerReviewRepository.countByProducerIdAndModerationStatus(userId, ReviewModerationStatus.ACTIVE);
        return score(followers, favorites, views, completed, rating, reviews);
    }

    public double studioScore(String studioId) {
        long followers = count(EngagementTargetType.STUDIO, studioId, EngagementAction.FOLLOW);
        long favorites = count(EngagementTargetType.STUDIO, studioId, EngagementAction.FAVORITE);
        long views = viewRepository.countByTargetTypeAndTargetId(EngagementTargetType.STUDIO, studioId);
        long completed = bookingRepository.countByStudioIdAndStatusAndPaymentStatus(studioId, BookingStatus.DELIVERED, BookingPaymentStatus.PAID);
        double rating = valueOrZero(studioRatingRepository.findAverageRatingByStudioId(studioId));
        long reviews = studioRatingRepository.countByStudioIdAndModerationStatus(studioId, ReviewModerationStatus.ACTIVE);
        return score(followers, favorites, views, completed, rating, reviews);
    }

    public double userTrendingScore(Integer userId) {
        return trendingScore(EngagementTargetType.USER, String.valueOf(userId));
    }

    public double studioTrendingScore(String studioId) {
        return trendingScore(EngagementTargetType.STUDIO, studioId);
    }

    private double trendingScore(EngagementTargetType type, String targetId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long follows = engagementRepository.countByTargetTypeAndTargetIdAndActionAndCreatedAtAfter(type, targetId, EngagementAction.FOLLOW, since);
        long favorites = engagementRepository.countByTargetTypeAndTargetIdAndActionAndCreatedAtAfter(type, targetId, EngagementAction.FAVORITE, since);
        long views = viewRepository.countByTargetTypeAndTargetIdAndCreatedAtAfter(type, targetId, since);
        return Math.round((follows * 5.0 + favorites * 3.0 + views * 0.5) * 100.0) / 100.0;
    }

    private long count(EngagementTargetType type, Object id, EngagementAction action) {
        return engagementRepository.countByTargetTypeAndTargetIdAndAction(type, String.valueOf(id), action);
    }

    private double score(long followers, long favorites, long views, long completed, double rating, long reviews) {
        return Math.round((followers * 3.0 + favorites * 2.0 + views * 0.25 + completed * 8.0 + rating * reviews * 2.0) * 100.0) / 100.0;
    }

    private double average(Double first, Double second) {
        if (first == null) return valueOrZero(second);
        if (second == null) return first;
        return (first + second) / 2.0;
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }
}

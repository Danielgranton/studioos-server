package com.studioos.server.reviews;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

public interface ProducerReviewRepository extends JpaRepository<ProducerReview, String> {
    @EntityGraph(attributePaths = "reviewer")
    Page<ProducerReview> findByProducerIdAndModerationStatus(Integer producerId, ReviewModerationStatus status, Pageable pageable);

    List<ProducerReview> findByProducerIdAndModerationStatus(Integer producerId, ReviewModerationStatus status);
    List<ProducerReview> findByProducerId(Integer producerId);
    Optional<ProducerReview> findByUserIdAndProducerId(Integer userId, Integer producerId);

    Optional<ProducerReview> findByIdAndModerationStatus(String id, ReviewModerationStatus status);

    @Query("SELECT AVG(r.rating) FROM ProducerReview r WHERE r.producerId = :producerId AND r.moderationStatus = com.studioos.server.reviews.ReviewModerationStatus.ACTIVE")
    Double findAverageRatingByProducerId(Integer producerId);

    @Query("SELECT AVG(r.rating) FROM ProducerReview r WHERE r.moderationStatus = com.studioos.server.reviews.ReviewModerationStatus.ACTIVE")
    Double findAverageRating();

    long countByProducerIdAndModerationStatus(Integer producerId, ReviewModerationStatus status);
    long countByProducerId(Integer producerId);
}

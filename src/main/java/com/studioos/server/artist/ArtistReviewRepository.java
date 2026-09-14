package com.studioos.server.artist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import com.studioos.server.reviews.ReviewModerationStatus;

public interface ArtistReviewRepository extends JpaRepository<ArtistReview, String> {
    @EntityGraph(attributePaths = "reviewer")
    Page<ArtistReview> findByArtistIdAndModerationStatus(Integer artistId, ReviewModerationStatus status, Pageable pageable);

    List<ArtistReview> findByArtistIdAndModerationStatus(Integer artistId, ReviewModerationStatus status);
    List<ArtistReview> findByArtistId(Integer artistId);
    Optional<ArtistReview> findByReviewerIdAndArtistId(Integer reviewerId, Integer artistId);

    Optional<ArtistReview> findByIdAndModerationStatus(String id, ReviewModerationStatus status);

    @Query("SELECT AVG(r.rating) FROM ArtistReview r WHERE r.artistId = :artistId AND r.moderationStatus = com.studioos.server.reviews.ReviewModerationStatus.ACTIVE")
    Double findAverageRatingByArtistId(Integer artistId);

    long countByArtistIdAndModerationStatus(Integer artistId, ReviewModerationStatus status);
    long countByArtistId(Integer artistId);
}

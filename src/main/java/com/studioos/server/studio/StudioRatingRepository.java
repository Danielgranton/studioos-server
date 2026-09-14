package com.studioos.server.studio;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.studioos.server.reviews.ReviewModerationStatus;

@Repository
public interface StudioRatingRepository extends JpaRepository<StudioRating, String> {

    Optional<StudioRating> findByStudioIdAndUserId(String studioId, Integer userId);

    @EntityGraph(attributePaths = "user")
    Page<StudioRating> findByStudioIdAndModerationStatus(String studioId, ReviewModerationStatus status, Pageable pageable);
    Page<StudioRating> findByStudioId(String studioId, Pageable pageable);

    Optional<StudioRating> findByIdAndModerationStatus(String id, ReviewModerationStatus status);

    @Query("SELECT AVG(r.rating) FROM StudioRating r WHERE r.studioId = :studioId AND r.moderationStatus = com.studioos.server.reviews.ReviewModerationStatus.ACTIVE")
    Double findAverageRatingByStudioId(String studioId);

    long countByStudioIdAndModerationStatus(String studioId, ReviewModerationStatus status);
    long countByStudioId(String studioId);
}

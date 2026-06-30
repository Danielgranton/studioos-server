package com.studioos.server.studio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudioRatingRepository extends JpaRepository<StudioRating, String> {

    Optional<StudioRating> findByStudioIdAndUserId(String studioId, Integer userId);

    @Query("SELECT AVG(r.rating) FROM StudioRating r WHERE r.studioId = :studioId")
    Double findAverageRatingByStudioId(String studioId);

    long countByStudioId(String studioId);
}
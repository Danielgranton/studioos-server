package com.studioos.server.artist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArtistReviewRepository extends JpaRepository<ArtistReview, String> {
    List<ArtistReview> findByArtistId(Integer artistId);
    Optional<ArtistReview> findByReviewerIdAndArtistId(Integer reviewerId, Integer artistId);

    @Query("SELECT AVG(r.rating) FROM ArtistReview r WHERE r.artistId = :artistId")
    Double findAverageRatingByArtistId(Integer artistId);

    long countByArtistId(Integer artistId);
}

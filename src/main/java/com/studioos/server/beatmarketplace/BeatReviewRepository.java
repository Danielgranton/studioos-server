package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BeatReviewRepository extends JpaRepository<BeatReview, String> {
    List<BeatReview> findByBeatId(String beatId);
    Optional<BeatReview> findByUserIdAndBeatId(Integer userId, String beatId);
    List<BeatReview> findByBeatIdIn(List<String> beatIds);
    List<BeatReview> findByUserId (Integer userId);

    @Query("SELECT r.beatId AS beatId, AVG(r.rating) AS averageRating, COUNT(r.id) AS reviewCount "
            + "FROM BeatReview r WHERE r.beatId IN :beatIds GROUP BY r.beatId")
    List<BeatRatingProjection> findRatingsByBeatIds(@Param("beatIds") List<String> beatIds);
}

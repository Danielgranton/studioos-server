package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatReviewRepository extends JpaRepository<BeatReview, String> {
    List<BeatReview> findByBeatId(String beatId);
    Optional<BeatReview> findByUserIdAndBeatId(Integer userId, String beatId);
    List<BeatReview> findByBeatIdIn(List<String> beatIds);
}
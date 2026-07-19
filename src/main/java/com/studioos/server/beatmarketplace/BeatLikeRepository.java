package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatLikeRepository extends JpaRepository<BeatLike, String> {
    List<BeatLike> findByBeatId(String beatId);
    List<BeatLike> findByUserId(Integer userId);
    Optional<BeatLike> findByUserIdAndBeatId(Integer userId, String beatId);
    boolean existsByUserIdAndBeatId(Integer userId, String beatId);
    void deleteByUserIdAndBeatId(Integer userId, String beatId);
}
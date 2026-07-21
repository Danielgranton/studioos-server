package com.studioos.server.beatmarketplace;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatPlayHistoryRepository extends JpaRepository<BeatPlayHistory, String> {
    List<BeatPlayHistory> findByBeatId(String beatId);
    List<BeatPlayHistory> findByBeatIdInAndPlayedAtAfter(List<String> beatIds, LocalDateTime playedAt);
    List<BeatPlayHistory> findByUserId(Integer userId);
    List<BeatPlayHistory> findTop10ByUserIdOrderByPlayedAtDesc(Integer userId);
    long countByBeatIdIn(List<String> beatIds);
}

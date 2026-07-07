package com.studioos.server.beatmarketplace;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatPlayHistoryRepository extends JpaRepository<BeatPlayHistory, String> {
    List<BeatPlayHistory> findByBeatId(String beatId);
    List<BeatPlayHistory> findByUserId(Integer userId);
    long countByBeatIdIn(List<String> beatIds);
}
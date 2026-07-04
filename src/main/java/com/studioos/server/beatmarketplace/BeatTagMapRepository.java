package com.studioos.server.beatmarketplace;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatTagMapRepository extends JpaRepository<BeatTagMap, BeatTagMapId> {
    List<BeatTagMap> findByIdBeatId(String beatId);
    List<BeatTagMap> findByIdTagId(String tagId);
}
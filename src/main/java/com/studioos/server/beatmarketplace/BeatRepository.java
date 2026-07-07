package com.studioos.server.beatmarketplace;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;

public interface BeatRepository extends JpaRepository<Beat, String>, JpaSpecificationExecutor<Beat> {
    List<Beat> findByProducerId(Integer producerId);
    List<Beat> findByStudioId(String studioId);
    List<Beat> findByStatusAndVisibility(BeatStatus status, BeatVisibility visibility);
    List<Beat> findByGenreId(String genreId);
}
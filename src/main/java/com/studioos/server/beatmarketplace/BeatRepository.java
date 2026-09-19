package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;

public interface BeatRepository extends JpaRepository<Beat, String>, JpaSpecificationExecutor<Beat> {
    List<Beat> findByProducerId(Integer producerId);
    long countByProducerId(Integer producerId);
    List<Beat> findByStudioId(String studioId);
    boolean existsByStudioId(String studioId);
    List<Beat> findByStatusAndVisibility(BeatStatus status, BeatVisibility visibility);
    long countByStatusAndVisibility(BeatStatus status, BeatVisibility visibility);
    List<Beat> findByGenreId(String genreId);
    boolean existsByStudioIdAndTitleIgnoreCase(String studioId, String title);
    boolean existsByStudioIdAndTitleIgnoreCaseAndIdNot(String studioId, String title, String id);
    Optional<Beat> findTopByStudioIdAndTitleIgnoreCaseOrderByCreatedAtDesc(String studioId, String title);
}

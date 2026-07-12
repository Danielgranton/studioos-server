package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.MediaJobStatus;

public interface MediaProcessingJobRepository extends JpaRepository<MediaProcessingJob, String> {
    List<MediaProcessingJob> findByBeatId(String beatId);
    List<MediaProcessingJob> findByStatus(MediaJobStatus status);
    List<MediaProcessingJob> findByStatusInAndUpdatedAtBefore(List<MediaJobStatus> statuses, LocalDateTime updatedAt);
    Optional<MediaProcessingJob> findByExternalJobId(String externalJobId);
}

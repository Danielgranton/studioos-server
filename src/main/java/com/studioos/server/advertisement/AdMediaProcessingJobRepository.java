package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.MediaJobStatus;

public interface AdMediaProcessingJobRepository extends JpaRepository<AdMediaProcessingJob, String> {
    List<AdMediaProcessingJob> findByAdvertisementId(String advertisementId);
    List<AdMediaProcessingJob> findByStatusInAndUpdatedAtBefore(List<MediaJobStatus> statuses, LocalDateTime updatedAt);
    Optional<AdMediaProcessingJob> findByExternalJobId(String externalJobId);
}

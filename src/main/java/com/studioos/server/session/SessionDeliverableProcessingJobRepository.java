package com.studioos.server.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.studioos.server.shared.enums.MediaJobStatus;

@Repository
public interface SessionDeliverableProcessingJobRepository extends JpaRepository<SessionDeliverableProcessingJob, String> {
    Optional<SessionDeliverableProcessingJob> findByExternalJobId(String externalJobId);
    List<SessionDeliverableProcessingJob> findBySessionDeliverableId(String sessionDeliverableId);
    List<SessionDeliverableProcessingJob> findByStatusInAndUpdatedAtBefore(List<MediaJobStatus> statuses, LocalDateTime updatedAt);
}

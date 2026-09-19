package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.UploadFileType;
import com.studioos.server.shared.enums.UploadSessionStatus;

public interface UploadSessionRepository extends JpaRepository<UploadSession, String> {
    List<UploadSession> findByProducerId(Integer producerId);
    List<UploadSession> findByBeatId(String beatId);
    List<UploadSession> findByStatus(UploadSessionStatus status);
    List<UploadSession> findByStatusInAndExpiresAtBefore(
            List<UploadSessionStatus> statuses, java.time.LocalDateTime expiresAt);
    Optional<UploadSession> findTopByBeatIdAndFileTypeOrderByCreatedAtDesc(String beatId, UploadFileType fileType);
}

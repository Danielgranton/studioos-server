package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.studioos.server.shared.enums.UploadSessionStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeatUploadCleanupService {

    private final UploadSessionRepository uploadSessionRepository;
    private final BeatService beatService;

    @Scheduled(fixedDelayString = "${beat.upload.cleanup-interval-ms:300000}")
    public void cleanExpiredUploads() {
        uploadSessionRepository.findByStatusInAndExpiresAtBefore(
                        List.of(UploadSessionStatus.PENDING, UploadSessionStatus.FAILED, UploadSessionStatus.EXPIRED),
                        LocalDateTime.now())
                .stream()
                .map(UploadSession::getBeatId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(beatService::expireUnfinishedUpload);
    }
}

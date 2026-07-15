package com.studioos.server.shared.media;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.studioos.server.advertisement.AdMediaProcessingJobRepository;
import com.studioos.server.advertisement.AdvertisementUploadService;
import com.studioos.server.beatmarketplace.BeatService;
import com.studioos.server.beatmarketplace.MediaProcessingJobRepository;
import com.studioos.server.session.DeliverableService;
import com.studioos.server.session.SessionDeliverableProcessingJobRepository;
import com.studioos.server.shared.enums.MediaJobStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaJobOrchestratorService {

    private static final List<MediaJobStatus> IN_FLIGHT_STATUSES =
            List.of(MediaJobStatus.QUEUED, MediaJobStatus.RUNNING, MediaJobStatus.PENDING);

    private final MediaProcessingClient mediaProcessingClient;
    private final MediaProcessingJobRepository beatJobRepository;
    private final AdMediaProcessingJobRepository adJobRepository;
    private final SessionDeliverableProcessingJobRepository sessionDeliverableJobRepository;
    private final BeatService beatService;
    private final AdvertisementUploadService advertisementUploadService;
    private final DeliverableService deliverableService;

    @Value("${media.job.stale-after-minutes:2}")
    private long staleAfterMinutes;

    @Scheduled(fixedDelayString = "${media.job.poll-interval-ms:60000}")
    public void pollStaleJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleAfterMinutes);

        beatJobRepository.findByStatusInAndUpdatedAtBefore(IN_FLIGHT_STATUSES, cutoff)
                .forEach(job -> syncBeatJob(job.getExternalJobId()));

        adJobRepository.findByStatusInAndUpdatedAtBefore(IN_FLIGHT_STATUSES, cutoff)
                .forEach(job -> syncAdJob(job.getExternalJobId()));

        sessionDeliverableJobRepository.findByStatusInAndUpdatedAtBefore(IN_FLIGHT_STATUSES, cutoff)
                .forEach(job -> syncSessionDeliverableJob(job.getExternalJobId()));
    }

    public void handleCallback(MediaProcessingCallbackRequest request) {
        String jobId = request.resolvedJobId();
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId or externalJobId is required");
        }

        MediaJobResult result = MediaJobResult.builder()
                .jobId(jobId)
                .status(request.getStatus() == null ? MediaJobStatus.QUEUED : request.getStatus())
                .resultReference(request.getResultReference())
                .errorMessage(request.getErrorMessage())
                .build();

        dispatchResult(result);
    }

    private void syncBeatJob(String jobId) {
        try {
            dispatchResult(mediaProcessingClient.getJobStatus(jobId));
        } catch (Exception e) {
            log.warn("Beat job poll failed for {}: {}", jobId, e.getMessage());
        }
    }

    private void syncAdJob(String jobId) {
        try {
            dispatchResult(mediaProcessingClient.getJobStatus(jobId));
        } catch (Exception e) {
            log.warn("Ad job poll failed for {}: {}", jobId, e.getMessage());
        }
    }

    private void syncSessionDeliverableJob(String jobId) {
        try {
            dispatchResult(mediaProcessingClient.getJobStatus(jobId));
        } catch (Exception e) {
            log.warn("Session deliverable job poll failed for {}: {}", jobId, e.getMessage());
        }
    }

    private void dispatchResult(MediaJobResult result) {
        boolean handled = beatService.applyMediaJobResult(result);
        if (!handled) {
            handled = advertisementUploadService.applyMediaJobResult(result);
        }
        if (!handled) {
            handled = deliverableService.applyMediaJobResult(result);
        }

        if (!handled) {
            log.warn("Media job result {} did not match any known beat/ad job", result.getJobId());
        }
    }
}

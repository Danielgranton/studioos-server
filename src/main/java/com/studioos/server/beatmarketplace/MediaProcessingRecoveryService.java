package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.media.MediaProcessingClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProcessingRecoveryService {

    private static final List<MediaJobStatus> SUBMISSION_STATUSES =
            List.of(MediaJobStatus.PENDING, MediaJobStatus.SUBMITTING);
    private static final List<MediaJobStatus> POLL_STATUSES =
            List.of(MediaJobStatus.QUEUED, MediaJobStatus.RUNNING);
    private static final int MAX_SUBMISSION_ATTEMPTS = 10;
    private static final int STALE_SUBMISSION_MINUTES = 2;
    private static final int MAX_PROCESSING_MINUTES = 30;

    private final MediaProcessingJobRepository jobRepository;
    private final MediaProcessingClient mediaProcessingClient;
    private final BeatService beatService;

    @Scheduled(fixedDelayString = "${media.processing.recovery-interval-ms:5000}")
    public void recover() {
        expireStuckJobs();

        try {
            if (!mediaProcessingClient.health()) {
                return;
            }
        } catch (RuntimeException exception) {
            log.debug("Media service is unavailable; recovery will retry: {}", exception.getMessage());
            return;
        }

        submitPendingJobs();
        pollSubmittedJobs();
    }

    private void expireStuckJobs() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(MAX_PROCESSING_MINUTES);
        jobRepository.findByStatusInAndUpdatedAtBefore(POLL_STATUSES, expiredBefore).forEach(job -> {
            job.setStatus(MediaJobStatus.FAILED);
            job.setErrorMessage("Media processing exceeded the 30 minute time limit");
            jobRepository.save(job);
            beatService.failBeat(job.getBeatId());
            log.warn("Media job timed out: jobId={} beatId={} externalJobId={}",
                    job.getId(), job.getBeatId(), job.getExternalJobId());
        });
    }

    private void submitPendingJobs() {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(STALE_SUBMISSION_MINUTES);
        jobRepository.findByStatusIn(SUBMISSION_STATUSES).stream()
                .filter(job -> job.getStatus() == MediaJobStatus.PENDING
                        || job.getUpdatedAt() == null
                        || job.getUpdatedAt().isBefore(staleBefore))
                .forEach(this::submit);
    }

    private void submit(MediaProcessingJob job) {
        if (job.getAttemptCount() >= MAX_SUBMISSION_ATTEMPTS) {
            job.setStatus(MediaJobStatus.FAILED);
            job.setErrorMessage("Media service submission exceeded the retry limit");
            jobRepository.save(job);
            beatService.failBeat(job.getBeatId());
            return;
        }

        job.setStatus(MediaJobStatus.SUBMITTING);
        job.setAttemptCount(job.getAttemptCount() + 1);
        jobRepository.save(job);

        try {
            String externalJobId = mediaProcessingClient.submitJob(
                    job.getAssetReference(), job.getOperation().getOperationString(), job.getParametersJson());
            job.setExternalJobId(externalJobId);
            job.setStatus(MediaJobStatus.QUEUED);
            job.setErrorMessage(null);
            jobRepository.save(job);
        } catch (RuntimeException exception) {
            job.setStatus(MediaJobStatus.PENDING);
            job.setErrorMessage(exception.getMessage());
            jobRepository.save(job);
            log.warn("Media job submission failed for {}: {}", job.getId(), exception.getMessage());
        }
    }

    private void pollSubmittedJobs() {
        jobRepository.findByStatusIn(POLL_STATUSES).stream()
                .filter(job -> job.getExternalJobId() != null && !job.getExternalJobId().isBlank())
                .forEach(job -> {
                    try {
                        beatService.applyMediaJobResult(mediaProcessingClient.getJobStatus(job.getExternalJobId()));
                    } catch (RuntimeException exception) {
                        log.debug("Media job poll failed for {}: {}", job.getExternalJobId(), exception.getMessage());
                    }
                });
    }
}

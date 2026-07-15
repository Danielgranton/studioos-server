package com.studioos.server.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.shared.enums.DeliverableStatus;
import com.studioos.server.shared.enums.DeliverableType;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.enums.SessionTimelineAction;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.media.MediaJobResult;
import com.studioos.server.shared.media.MediaProcessingClient;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.shared.storage.StorageObjectMetadata;
import com.studioos.server.user.User;
import com.studioos.server.session.dto.SessionDeliverableUploadSessionResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverableService {

    private final SessionDeliverableRepository sessionDeliverableRepository;
    private final SessionDeliverableProcessingJobRepository sessionDeliverableProcessingJobRepository;
    private final RecordingSessionRepository recordingSessionRepository;
    private final SessionAccessService sessionAccessService;
    private final SessionTimelineService timelineService;
    private final PresignedUrlService presignedUrlService;
    private final MediaProcessingClient mediaProcessingClient;
    private final ApplicationEventPublisher applicationEventPublisher;
    @Value("${storage.s3.bucket}")
    private String mediaBucket;

    private static final int UPLOAD_URL_EXPIRY_SECONDS = 900;
    private static final long MAX_UPLOAD_BYTES = 500L * 1024L * 1024L;

    @Transactional
    public SessionDeliverableUploadSessionResponse startUpload(User currentUser, String sessionId, DeliverableType type,
                                                               String objectKey, String contentType,
                                                               Integer duration) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageProduction(session, currentUser);

        SessionDeliverable deliverable = sessionDeliverableRepository.save(SessionDeliverable.builder()
                .sessionId(sessionId)
                .type(type)
                .status(DeliverableStatus.UPLOADING)
                .bucket(mediaBucket)
                .objectKey(objectKey)
                .contentType(contentType)
                .duration(duration)
                .uploadedBy(currentUser.getId())
                .expiresAt(LocalDateTime.now().plusSeconds(UPLOAD_URL_EXPIRY_SECONDS))
                .build());

        timelineService.recordEvent(sessionId, SessionTimelineAction.DELIVERABLE_UPLOAD_CREATED,
                currentUser.getId(), "Deliverable upload session created: " + type.name());

        String uploadUrl = presignedUrlService.generateUploadUrl(
                mediaBucket, objectKey, contentType, UPLOAD_URL_EXPIRY_SECONDS);

        return SessionDeliverableUploadSessionResponse.builder()
                .deliverableId(deliverable.getId())
                .uploadUrl(uploadUrl)
                .status(deliverable.getStatus())
                .type(deliverable.getType())
                .expiresAt(deliverable.getExpiresAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SessionDeliverable> listDeliverables(String sessionId) {
        return sessionDeliverableRepository.findBySessionId(sessionId);
    }

    @Transactional
    public SessionDeliverable completeUpload(User currentUser, String sessionId, String deliverableId) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageProduction(session, currentUser);

        SessionDeliverable deliverable = requireDeliverable(deliverableId);
        if (!deliverable.getSessionId().equals(sessionId)) {
            throw StudioosException.forbidden("Deliverable does not belong to this session");
        }

        if (deliverable.getStatus() != DeliverableStatus.UPLOADING) {
            throw StudioosException.badRequest("Deliverable is not awaiting upload");
        }

        Optional<StorageObjectMetadata> metadata = presignedUrlService.objectMetadata(
                deliverable.getBucket(), deliverable.getObjectKey());
        StorageObjectMetadata objectMetadata = metadata.orElseThrow(
                () -> StudioosException.badRequest("Uploaded file not found in storage"));

        if (objectMetadata.contentLength() == null || objectMetadata.contentLength() <= 0) {
            throw StudioosException.badRequest("Uploaded file has no content length");
        }
        if (objectMetadata.contentLength() > MAX_UPLOAD_BYTES) {
            throw StudioosException.badRequest("Uploaded file exceeds the allowed size");
        }

        deliverable.setStatus(DeliverableStatus.PROCESSING);
        sessionDeliverableRepository.save(deliverable);
        timelineService.recordEvent(sessionId, SessionTimelineAction.DELIVERABLE_UPLOAD_COMPLETED,
                currentUser.getId(), "Deliverable upload completed: " + deliverable.getType().name());

        submitProcessingJobs(deliverable);
        return deliverable;
    }

    @Transactional
    public boolean applyMediaJobResult(MediaJobResult result) {
        SessionDeliverableProcessingJob job = sessionDeliverableProcessingJobRepository.findByExternalJobId(result.getJobId())
                .orElse(null);
        if (job == null) {
            return false;
        }

        if (job.getStatus().isTerminal()) {
            return true;
        }

        MediaJobStatus nextStatus = result.getStatus() == null ? MediaJobStatus.QUEUED : result.getStatus();
        job.setStatus(nextStatus);
        job.setResultReference(result.getResultReference());
        job.setErrorMessage(result.getErrorMessage());
        sessionDeliverableProcessingJobRepository.save(job);

        SessionDeliverable deliverable = requireDeliverable(job.getSessionDeliverableId());
        if (nextStatus == MediaJobStatus.FAILED) {
            deliverable.setStatus(DeliverableStatus.FAILED);
            sessionDeliverableRepository.save(deliverable);
            timelineService.recordEvent(deliverable.getSessionId(), SessionTimelineAction.DELIVERABLE_PROCESSING_FAILED,
                    deliverable.getUploadedBy(), result.getErrorMessage());
            return true;
        }

        if (nextStatus == MediaJobStatus.SUCCESS) {
            finalizeIfReady(deliverable);
        }
        return true;
    }

    @Transactional
    public void deleteDeliverable(String deliverableId) {
        sessionDeliverableRepository.deleteById(deliverableId);
    }

    private void submitProcessingJobs(SessionDeliverable deliverable) {
        List<String> operations = operationsFor(deliverable.getType());
        if (operations.isEmpty()) {
            deliverable.setStatus(DeliverableStatus.READY);
            sessionDeliverableRepository.save(deliverable);
            timelineService.recordEvent(deliverable.getSessionId(), SessionTimelineAction.DELIVERABLE_PROCESSING_COMPLETED,
                    deliverable.getUploadedBy(), "Deliverable is ready without extra processing");
            return;
        }

        for (String operation : operations) {
            String externalJobId = mediaProcessingClient.submitJob(
                    deliverable.getObjectKey(), operation, "{}");

            sessionDeliverableProcessingJobRepository.save(SessionDeliverableProcessingJob.builder()
                    .sessionDeliverableId(deliverable.getId())
                    .operation(operation)
                    .externalJobId(externalJobId)
                    .status(MediaJobStatus.QUEUED)
                    .build());
        }
    }

    private void finalizeIfReady(SessionDeliverable deliverable) {
        List<SessionDeliverableProcessingJob> jobs = sessionDeliverableProcessingJobRepository.findBySessionDeliverableId(deliverable.getId());
        boolean anyFailed = jobs.stream().anyMatch(job -> job.getStatus() == MediaJobStatus.FAILED);
        if (anyFailed) {
            deliverable.setStatus(DeliverableStatus.FAILED);
            sessionDeliverableRepository.save(deliverable);
            timelineService.recordEvent(deliverable.getSessionId(), SessionTimelineAction.DELIVERABLE_PROCESSING_FAILED,
                    deliverable.getUploadedBy(), "One or more media jobs failed");
            return;
        }

        boolean allDone = jobs.isEmpty() || jobs.stream().allMatch(job -> job.getStatus() == MediaJobStatus.SUCCESS);
        if (!allDone) {
            return;
        }

        for (SessionDeliverableProcessingJob job : jobs) {
            String ref = job.getResultReference();
            if (ref == null || ref.isBlank()) {
                continue;
            }
            switch (job.getOperation()) {
                case "audio.normalize" -> deliverable.setOriginalFileId(ref);
                case "audio.generatePreview" -> deliverable.setPreviewFileId(ref);
                case "audio.generateWaveform" -> deliverable.setThumbnailId(ref);
                case "image.resize", "video.compress" -> deliverable.setOriginalFileId(ref);
                case "image.generateThumbnail", "video.generateThumbnail" -> deliverable.setThumbnailId(ref);
                case "image.convertWebp" -> deliverable.setPreviewFileId(ref);
                default -> log.debug("Ignoring media result for unsupported deliverable op {}", job.getOperation());
            }
        }

        deliverable.setStatus(DeliverableStatus.READY);
        sessionDeliverableRepository.save(deliverable);
        timelineService.recordEvent(deliverable.getSessionId(), SessionTimelineAction.DELIVERABLE_PROCESSING_COMPLETED,
                deliverable.getUploadedBy(), "Deliverable processing completed");
    }

    private List<String> operationsFor(DeliverableType type) {
        return switch (type) {
            case RAW_AUDIO, MIX, MASTER, STEMS -> List.of(
                    "audio.normalize",
                    "audio.generatePreview",
                    "audio.generateWaveform");
            case VIDEO -> List.of(
                    "video.compress",
                    "video.generateThumbnail");
            case LYRICS, PROJECT_FILE -> List.of();
        };
    }

    private RecordingSession requireSession(String sessionId) {
        return recordingSessionRepository.findById(sessionId)
                .orElseThrow(() -> StudioosException.notFound("Session not found"));
    }

    private SessionDeliverable requireDeliverable(String deliverableId) {
        return sessionDeliverableRepository.findById(deliverableId)
                .orElseThrow(() -> StudioosException.notFound("Deliverable not found"));
    }
}

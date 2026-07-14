package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.studioos.server.beatmarketplace.BeatStorageConstants.BEAT_UPLOAD_PREFIX;
import com.studioos.server.beatmarketplace.dto.BeatUploadCompleteResponse;
import com.studioos.server.beatmarketplace.dto.BeatUploadSessionResponse;
import com.studioos.server.beatmarketplace.dto.CreateBeatRequest;
import com.studioos.server.beatmarketplace.dto.MediaJobCallbackRequest;
import com.studioos.server.beatmarketplace.dto.RefreshUploadSessionResponse;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.search.event.BeatCreatedEvent;
import com.studioos.server.search.event.BeatUpdatedEvent;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.MediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.shared.enums.UploadFileType;
import com.studioos.server.shared.enums.UploadSessionStatus;
import com.studioos.server.shared.media.MediaJobResult;
import com.studioos.server.shared.media.MediaProcessingClient;
import com.studioos.server.shared.storage.StorageObjectMetadata;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeatService {

    private static final int UPLOAD_URL_EXPIRY_SECONDS = 900; // 15 minutes
    private static final int MIN_BPM = 40;
    private static final int MAX_BPM = 300;
    private static final long MAX_AUDIO_BYTES = 200L * 1024L * 1024L;
    private static final long MAX_COVER_BYTES = 10L * 1024L * 1024L;

    private final BeatRepository beatRepository;
    private final BeatGenreRepository beatGenreRepository;
    private final StudioRepository studioRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final MediaProcessingJobRepository mediaProcessingJobRepository;
    private final PresignedUrlService presignedUrlService;
    private final MediaProcessingClient mediaProcessingClient;
    private final NotificationServiceImpl notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    @Value("${storage.s3.bucket}")
    private String mediaBucket;

    @Transactional
    public BeatUploadSessionResponse createDraftAndUploadSessions(Integer producerId, CreateBeatRequest request) {

        beatGenreRepository.findById(request.getGenreId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid genreId: " + request.getGenreId()));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new IllegalArgumentException("Studio not found: " + request.getStudioId()));
        if (!studio.getOwnerId().equals(producerId)) {
            throw new SecurityException("Producer does not own this studio");
        }

        if (request.getBpm() != null && (request.getBpm() < MIN_BPM || request.getBpm() > MAX_BPM)) {
            throw new IllegalArgumentException("BPM must be between " + MIN_BPM + " and " + MAX_BPM);
        }

        String normalizedTitle = normalizeTitle(request.getTitle());
        if (beatRepository.existsByStudioIdAndTitleIgnoreCase(request.getStudioId(), normalizedTitle)) {
            throw new IllegalStateException("A beat with this title already exists in the studio");
        }

        Beat beat = Beat.builder()
                .producerId(producerId)
                .studioId(request.getStudioId())
                .title(normalizedTitle)
                .description(request.getDescription())
                .genreId(request.getGenreId())
                .bpm(request.getBpm())
                .keySignature(request.getKeySignature())
                .mood(request.getMood())
                .status(BeatStatus.UPLOADING)
                .visibility(request.getVisibility())
                .build();

        beat = beatRepository.save(beat);
        applicationEventPublisher.publishEvent(new BeatCreatedEvent(beat.getId()));

        String audioKey = "%s/beat_%s_original.mp3".formatted(BEAT_UPLOAD_PREFIX, beat.getId());
        String coverKey = "%s/cover_%s_original.jpg".formatted(BEAT_UPLOAD_PREFIX, beat.getId());

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(UPLOAD_URL_EXPIRY_SECONDS);

        UploadSession audioSession = UploadSession.builder()
                .producerId(producerId)
                .beatId(beat.getId())
                .bucket(mediaBucket)
                .objectKey(audioKey)
                .fileType(UploadFileType.AUDIO)
                .contentType("audio/mpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        UploadSession coverSession = UploadSession.builder()
                .producerId(producerId)
                .beatId(beat.getId())
                .bucket(mediaBucket)
                .objectKey(coverKey)
                .fileType(UploadFileType.COVER)
                .contentType("image/jpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        audioSession = uploadSessionRepository.save(audioSession);
        coverSession = uploadSessionRepository.save(coverSession);

        String audioUploadUrl = presignedUrlService.generateUploadUrl(
                mediaBucket, audioKey, "audio/mpeg", UPLOAD_URL_EXPIRY_SECONDS);
        String coverUploadUrl = presignedUrlService.generateUploadUrl(
                mediaBucket, coverKey, "image/jpeg", UPLOAD_URL_EXPIRY_SECONDS);

        return BeatUploadSessionResponse.builder()
                .beatId(beat.getId())
                .beatUploadUrl(audioUploadUrl)
                .coverUploadUrl(coverUploadUrl)
                .beatUploadSessionId(audioSession.getId())
                .coverUploadSessionId(coverSession.getId())
                .build();
    }

    @Transactional
    public BeatUploadCompleteResponse completeUpload(Integer producerId, String beatId) {

        Beat beat = beatRepository.findById(beatId)
                .orElseThrow(() -> new IllegalArgumentException("Beat not found: " + beatId));

        if (!beat.getProducerId().equals(producerId)) {
            throw new SecurityException("Producer does not own this beat upload");
        }

        if (beat.getStatus() != BeatStatus.UPLOADING) {
            throw new IllegalStateException(
                    "Upload already completed or beat is in an unexpected state: " + beat.getStatus());
        }

        UploadSession audioSession = uploadSessionRepository
                .findTopByBeatIdAndFileTypeOrderByCreatedAtDesc(beatId, UploadFileType.AUDIO)
                .orElseThrow(() -> new IllegalStateException("Missing audio upload session for beat " + beatId));
        UploadSession coverSession = uploadSessionRepository
                .findTopByBeatIdAndFileTypeOrderByCreatedAtDesc(beatId, UploadFileType.COVER)
                .orElseThrow(() -> new IllegalStateException("Missing cover upload session for beat " + beatId));

        if (audioSession.getStatus() != UploadSessionStatus.PENDING
                || coverSession.getStatus() != UploadSessionStatus.PENDING) {
            throw new IllegalStateException("Upload already verified for beat " + beatId);
        }

        StorageObjectMetadata audioMetadata = requireUploadedObject(
                audioSession, MAX_AUDIO_BYTES, "Audio file not found in storage for beat " + beatId);
        StorageObjectMetadata coverMetadata = requireUploadedObject(
                coverSession, MAX_COVER_BYTES, "Cover file not found in storage for beat " + beatId);

        audioSession.setStatus(UploadSessionStatus.VERIFIED);
        audioSession.setSizeBytes(audioMetadata.contentLength());
        audioSession.setChecksum(normalizeChecksum(audioMetadata.eTag()));
        coverSession.setStatus(UploadSessionStatus.VERIFIED);
        coverSession.setSizeBytes(coverMetadata.contentLength());
        coverSession.setChecksum(normalizeChecksum(coverMetadata.eTag()));
        uploadSessionRepository.save(audioSession);
        uploadSessionRepository.save(coverSession);

        beat.setStatus(BeatStatus.PROCESSING);
        beat = beatRepository.save(beat);
        applicationEventPublisher.publishEvent(new BeatUpdatedEvent(beat.getId()));

        submitProcessingJobs(beat, audioSession, coverSession);

        return BeatUploadCompleteResponse.builder()
                .beatId(beat.getId())
                .status(beat.getStatus())
                .build();
    }

    @Transactional
    public RefreshUploadSessionResponse refreshUploadSessions(Integer producerId, String beatId) {

        Beat beat = beatRepository.findById(beatId)
                .orElseThrow(() -> new IllegalArgumentException("Beat not found: " + beatId));

        if (!beat.getProducerId().equals(producerId)) {
            throw new SecurityException("Producer does not own this beat upload");
        }

        if (beat.getStatus() != BeatStatus.UPLOADING) {
            throw new IllegalStateException(
                    "Cannot refresh upload sessions — beat is in state: " + beat.getStatus() +
                    ". Only beats still awaiting upload can be refreshed.");
        }

        List<UploadSession> oldSessions = uploadSessionRepository.findByBeatId(beatId);
        oldSessions.forEach(s -> {
            if (s.getStatus() == UploadSessionStatus.PENDING || s.getStatus() == UploadSessionStatus.FAILED) {
                s.setStatus(UploadSessionStatus.EXPIRED);
            }
        });
        uploadSessionRepository.saveAll(oldSessions);

        String audioKey = "%s/beat_%s_original.mp3".formatted(BEAT_UPLOAD_PREFIX, beat.getId());
        String coverKey = "%s/cover_%s_original.jpg".formatted(BEAT_UPLOAD_PREFIX, beat.getId());

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(UPLOAD_URL_EXPIRY_SECONDS);

        UploadSession newAudioSession = UploadSession.builder()
                .producerId(producerId)
                .beatId(beat.getId())
                .bucket(mediaBucket)
                .objectKey(audioKey)
                .fileType(UploadFileType.AUDIO)
                .contentType("audio/mpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        UploadSession newCoverSession = UploadSession.builder()
                .producerId(producerId)
                .beatId(beat.getId())
                .bucket(mediaBucket)
                .objectKey(coverKey)
                .fileType(UploadFileType.COVER)
                .contentType("image/jpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        newAudioSession = uploadSessionRepository.save(newAudioSession);
        newCoverSession = uploadSessionRepository.save(newCoverSession);

        String audioUploadUrl = presignedUrlService.generateUploadUrl(
                mediaBucket, audioKey, "audio/mpeg", UPLOAD_URL_EXPIRY_SECONDS);
        String coverUploadUrl = presignedUrlService.generateUploadUrl(
                mediaBucket, coverKey, "image/jpeg", UPLOAD_URL_EXPIRY_SECONDS);

        return RefreshUploadSessionResponse.builder()
                .beatId(beat.getId())
                .beatUploadUrl(audioUploadUrl)
                .coverUploadUrl(coverUploadUrl)
                .beatUploadSessionId(newAudioSession.getId())
                .coverUploadSessionId(newCoverSession.getId())
                .build();
    }

    @Transactional
    public void handleJobCallback(MediaJobCallbackRequest callback) {
        MediaJobResult result = MediaJobResult.builder()
                .jobId(callback.getExternalJobId())
                .status(callback.isSuccess() ? MediaJobStatus.SUCCESS : MediaJobStatus.FAILED)
                .resultReference(callback.getResultReference())
                .errorMessage(callback.getErrorMessage())
                .build();
        applyMediaJobResult(result);
    }

    @Transactional
    public boolean applyMediaJobResult(MediaJobResult result) {
        MediaProcessingJob job = mediaProcessingJobRepository.findByExternalJobId(result.getJobId())
                .orElse(null);
        if (job == null) {
            return false;
        }

        if (job.getStatus().isTerminal()) {
            log.debug("Ignoring duplicate media result for job {} (already {})", job.getId(), job.getStatus());
            return true;
        }

        MediaJobStatus nextStatus = result.getStatus() == null ? MediaJobStatus.QUEUED : result.getStatus();
        job.setStatus(nextStatus);
        job.setResultReference(result.getResultReference());
        job.setErrorMessage(result.getErrorMessage());
        mediaProcessingJobRepository.save(job);

        if (nextStatus == MediaJobStatus.FAILED) {
            log.error("Media job failed: beat={} operation={} error={}",
                    job.getBeatId(), job.getOperation(), result.getErrorMessage());
            failBeat(job.getBeatId());
            return true;
        }

        if (nextStatus == MediaJobStatus.SUCCESS) {
            checkAndFinalizeBeat(job.getBeatId());
        }
        return true;
    }

    private void submitProcessingJobs(Beat beat, UploadSession audioSession, UploadSession coverSession) {
        submitAndTrack(beat.getId(), MediaJobOperation.AUDIO_NORMALIZE, audioSession.getObjectKey(), "{}");
        submitAndTrack(beat.getId(), MediaJobOperation.AUDIO_PREVIEW, audioSession.getObjectKey(),
                "{\"start\":\"00:00:00\",\"end\":\"00:00:30\"}");
        submitAndTrack(beat.getId(), MediaJobOperation.AUDIO_WAVEFORM, audioSession.getObjectKey(), "{}");
        submitAndTrack(beat.getId(), MediaJobOperation.COVER_RESIZE, coverSession.getObjectKey(),
                "{\"width\":1000,\"height\":1000}");
        submitAndTrack(beat.getId(), MediaJobOperation.COVER_THUMBNAIL, coverSession.getObjectKey(), "{}");
        submitAndTrack(beat.getId(), MediaJobOperation.COVER_WEBP, coverSession.getObjectKey(), "{}");
    }

    private void submitAndTrack(String beatId, MediaJobOperation operation, String assetReference, String parametersJson) {
        String externalJobId = mediaProcessingClient.submitJob(
                assetReference, operation.getOperationString(), parametersJson);

        MediaProcessingJob job = MediaProcessingJob.builder()
                .beatId(beatId)
                .operation(operation)
                .externalJobId(externalJobId)
                .status(MediaJobStatus.QUEUED)
                .build();

        mediaProcessingJobRepository.save(job);
    }

    private void failBeat(String beatId) {
        beatRepository.findById(beatId).ifPresent(beat -> {
            if (beat.getStatus() == BeatStatus.PROCESSING) {
                beat.setStatus(BeatStatus.FAILED);
                beatRepository.save(beat);
                notifyProducer(
                        beat.getProducerId(),
                        NotificationType.BEAT_PROCESSING_FAILED,
                        "Beat processing failed",
                        "Your beat \"" + beat.getTitle() + "\" could not be processed. Please review the upload and try again.",
                        beat.getId());
            }
        });
    }

    private void checkAndFinalizeBeat(String beatId) {
        List<MediaProcessingJob> jobs = mediaProcessingJobRepository.findByBeatId(beatId);

        boolean anyFailed = jobs.stream().anyMatch(j -> j.getStatus() == MediaJobStatus.FAILED);
        if (anyFailed) {
            failBeat(beatId);
            return;
        }

        boolean allDone = jobs.stream().allMatch(j -> j.getStatus() == MediaJobStatus.SUCCESS);
        if (!allDone) {
            return; // still waiting on sibling jobs for this beat
        }

        beatRepository.findById(beatId).ifPresent(beat -> {
            if (beat.getStatus() != BeatStatus.PROCESSING) {
                return; // already finalized — avoid double-processing on a race
            }

            for (MediaProcessingJob job : jobs) {
                String ref = job.getResultReference();
                switch (job.getOperation()) {
                    case AUDIO_NORMALIZE -> beat.setAudioUrl(ref);
                    case AUDIO_PREVIEW -> beat.setPreviewUrl(ref);
                    case AUDIO_WAVEFORM -> beat.setWaveformUrl(ref);
                    case COVER_THUMBNAIL -> beat.setThumbnailUrl(ref);
                    case COVER_WEBP -> beat.setCoverUrl(ref);
                    case COVER_RESIZE -> { /* intermediate step only */ }
                }
            }

            beat.setStatus(BeatStatus.READY);
            beatRepository.save(beat);
            applicationEventPublisher.publishEvent(new BeatUpdatedEvent(beat.getId()));
            notifyProducer(
                    beat.getProducerId(),
                    NotificationType.BEAT_PROCESSING_COMPLETED,
                    "Beat processing completed",
                    "Your beat \"" + beat.getTitle() + "\" is ready.",
                    beat.getId());
        });
    }

    private void notifyProducer(Integer producerId, NotificationType type, String title, String message,
                                String relatedEntityId) {
        try {
            CreateNotificationRequest request = new CreateNotificationRequest();
            request.setUserId(producerId);
            request.setType(type);
            request.setTitle(title);
            request.setMessage(message);
            request.setRelatedEntityId(relatedEntityId);
            notificationService.createNotification(request);
        } catch (Exception e) {
            log.error("Failed to notify producer {} about beat event {}: {}", producerId, type, e.getMessage());
        }
    }

    private StorageObjectMetadata requireUploadedObject(
            UploadSession session,
            long maxBytes,
            String notFoundMessage) {

        Optional<StorageObjectMetadata> metadata = presignedUrlService.objectMetadata(
                session.getBucket(), session.getObjectKey());

        StorageObjectMetadata objectMetadata = metadata.orElseThrow(() -> new IllegalStateException(notFoundMessage));

        if (objectMetadata.contentLength() == null || objectMetadata.contentLength() <= 0) {
            throw new IllegalStateException("Uploaded file has no content length for session " + session.getId());
        }

        if (objectMetadata.contentLength() > maxBytes) {
            throw new IllegalStateException("Uploaded file exceeds the allowed size for session " + session.getId());
        }

        if (session.getContentType() != null && !session.getContentType().isBlank()
                && objectMetadata.contentType() != null
                && !session.getContentType().equalsIgnoreCase(objectMetadata.contentType())) {
            throw new IllegalStateException(
                    "Uploaded file content type does not match the expected type for session " + session.getId());
        }

        return objectMetadata;
    }

    private String normalizeChecksum(String checksum) {
        if (checksum == null) {
            return null;
        }
        String trimmed = checksum.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String normalizeTitle(String title) {
        return title == null ? null : title.trim().replaceAll("\\s+", " ");
    }
}

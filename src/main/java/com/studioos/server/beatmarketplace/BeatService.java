package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.dto.BeatUploadCompleteResponse;
import com.studioos.server.beatmarketplace.dto.BeatUploadSessionResponse;
import com.studioos.server.beatmarketplace.dto.CreateBeatRequest;
import com.studioos.server.beatmarketplace.dto.MediaJobCallbackRequest;
import com.studioos.server.beatmarketplace.dto.RefreshUploadSessionResponse;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.MediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.enums.UploadFileType;
import com.studioos.server.shared.enums.UploadSessionStatus;
import com.studioos.server.shared.media.MediaProcessingClient;
import com.studioos.server.shared.storage.PresignedUrlService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeatService {

    private static final String MEDIA_BUCKET = "studioos-media";
    private static final String BEAT_UPLOAD_PREFIX = "beats/uploads";
    private static final int UPLOAD_URL_EXPIRY_SECONDS = 900; // 15 minutes

    private final BeatRepository beatRepository;
    private final BeatGenreRepository beatGenreRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final MediaProcessingJobRepository mediaProcessingJobRepository;
    private final PresignedUrlService presignedUrlService;
    private final MediaProcessingClient mediaProcessingClient;

    @Transactional
    public BeatUploadSessionResponse createDraftAndUploadSessions(Integer producerId, CreateBeatRequest request) {

        beatGenreRepository.findById(request.getGenreId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid genreId: " + request.getGenreId()));

        // TODO: validate producer owns the studio (once StudioService exposes an ownership check)
        // TODO: validate BPM range, optional title-uniqueness check per your original design notes

        Beat beat = Beat.builder()
                .producerId(producerId)
                .studioId(request.getStudioId())
                .title(request.getTitle())
                .description(request.getDescription())
                .genreId(request.getGenreId())
                .bpm(request.getBpm())
                .keySignature(request.getKeySignature())
                .mood(request.getMood())
                .status(BeatStatus.UPLOADING)
                .visibility(request.getVisibility())
                .build();

        beat = beatRepository.save(beat);

        String audioKey = "%s/beat_%s_original.mp3".formatted(BEAT_UPLOAD_PREFIX, beat.getId());
        String coverKey = "%s/cover_%s_original.jpg".formatted(BEAT_UPLOAD_PREFIX, beat.getId());

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(UPLOAD_URL_EXPIRY_SECONDS);

        UploadSession audioSession = UploadSession.builder()
                .producerId(producerId)
                .beatId(beat.getId())
                .bucket(MEDIA_BUCKET)
                .objectKey(audioKey)
                .fileType(UploadFileType.AUDIO)
                .contentType("audio/mpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        UploadSession coverSession = UploadSession.builder()
                .producerId(producerId)
                .beatId(beat.getId())
                .bucket(MEDIA_BUCKET)
                .objectKey(coverKey)
                .fileType(UploadFileType.COVER)
                .contentType("image/jpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        audioSession = uploadSessionRepository.save(audioSession);
        coverSession = uploadSessionRepository.save(coverSession);

        String audioUploadUrl = presignedUrlService.generateUploadUrl(
                MEDIA_BUCKET, audioKey, "audio/mpeg", UPLOAD_URL_EXPIRY_SECONDS);
        String coverUploadUrl = presignedUrlService.generateUploadUrl(
                MEDIA_BUCKET, coverKey, "image/jpeg", UPLOAD_URL_EXPIRY_SECONDS);

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

        List<UploadSession> sessions = uploadSessionRepository.findByBeatId(beatId);

        UploadSession audioSession = sessions.stream()
                .filter(s -> s.getFileType() == UploadFileType.AUDIO)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing audio upload session for beat " + beatId));

        UploadSession coverSession = sessions.stream()
                .filter(s -> s.getFileType() == UploadFileType.COVER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing cover upload session for beat " + beatId));

        if (audioSession.getStatus() == UploadSessionStatus.VERIFIED
                || coverSession.getStatus() == UploadSessionStatus.VERIFIED) {
            throw new IllegalStateException("Upload already verified for beat " + beatId);
        }

        boolean audioExists = presignedUrlService.objectExists(audioSession.getBucket(), audioSession.getObjectKey());
        boolean coverExists = presignedUrlService.objectExists(coverSession.getBucket(), coverSession.getObjectKey());

        if (!audioExists) {
            audioSession.setStatus(UploadSessionStatus.FAILED);
            uploadSessionRepository.save(audioSession);
            throw new IllegalStateException("Audio file not found in storage for beat " + beatId);
        }

        if (!coverExists) {
            coverSession.setStatus(UploadSessionStatus.FAILED);
            uploadSessionRepository.save(coverSession);
            throw new IllegalStateException("Cover file not found in storage for beat " + beatId);
        }

        // TODO: validate file sizes once objectExists/head-object call returns real metadata (post-S3 wiring)

        audioSession.setStatus(UploadSessionStatus.VERIFIED);
        coverSession.setStatus(UploadSessionStatus.VERIFIED);
        uploadSessionRepository.save(audioSession);
        uploadSessionRepository.save(coverSession);

        beat.setStatus(BeatStatus.PROCESSING);
        beat = beatRepository.save(beat);

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
                .bucket(MEDIA_BUCKET)
                .objectKey(audioKey)
                .fileType(UploadFileType.AUDIO)
                .contentType("audio/mpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        UploadSession newCoverSession = UploadSession.builder()
                .producerId(producerId)
                .beatId(beat.getId())
                .bucket(MEDIA_BUCKET)
                .objectKey(coverKey)
                .fileType(UploadFileType.COVER)
                .contentType("image/jpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        newAudioSession = uploadSessionRepository.save(newAudioSession);
        newCoverSession = uploadSessionRepository.save(newCoverSession);

        String audioUploadUrl = presignedUrlService.generateUploadUrl(
                MEDIA_BUCKET, audioKey, "audio/mpeg", UPLOAD_URL_EXPIRY_SECONDS);
        String coverUploadUrl = presignedUrlService.generateUploadUrl(
                MEDIA_BUCKET, coverKey, "image/jpeg", UPLOAD_URL_EXPIRY_SECONDS);

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

        MediaProcessingJob job = mediaProcessingJobRepository.findByExternalJobId(callback.getExternalJobId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown externalJobId: " + callback.getExternalJobId()));

        if (job.getStatus() != MediaJobStatus.PENDING) {
            log.warn("Ignoring duplicate callback for job {} (already {})", job.getId(), job.getStatus());
            return;
        }

        if (!callback.isSuccess()) {
            job.setStatus(MediaJobStatus.FAILED);
            job.setErrorMessage(callback.getErrorMessage());
            mediaProcessingJobRepository.save(job);
            log.error("Media job failed: beat={} operation={} error={}",
                    job.getBeatId(), job.getOperation(), callback.getErrorMessage());
            failBeat(job.getBeatId());
            return;
        }

        job.setStatus(MediaJobStatus.SUCCESS);
        job.setResultReference(callback.getResultReference());
        mediaProcessingJobRepository.save(job);

        checkAndFinalizeBeat(job.getBeatId());
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
                .status(MediaJobStatus.PENDING)
                .build();

        mediaProcessingJobRepository.save(job);
    }

    private void failBeat(String beatId) {
        beatRepository.findById(beatId).ifPresent(beat -> {
            if (beat.getStatus() == BeatStatus.PROCESSING) {
                beat.setStatus(BeatStatus.FAILED);
                beatRepository.save(beat);
                // TODO: notify producer of processing failure once NotificationService hook is wired here
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
                    case COVER_WEBP -> beat.setCoverUrl(ref);
                    case COVER_THUMBNAIL -> { /* TODO: Beat has no thumbnailUrl column yet */ }
                    case COVER_RESIZE -> { /* intermediate step only */ }
                }
            }

            beat.setStatus(BeatStatus.READY);
            beatRepository.save(beat);
            // TODO: notify producer that their beat finished processing
        });
    }
}
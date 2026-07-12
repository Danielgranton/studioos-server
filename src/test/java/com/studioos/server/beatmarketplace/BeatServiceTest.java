package com.studioos.server.beatmarketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.studioos.server.beatmarketplace.dto.BeatUploadCompleteResponse;
import com.studioos.server.beatmarketplace.dto.CreateBeatRequest;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;
import com.studioos.server.shared.enums.MediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.enums.UploadFileType;
import com.studioos.server.shared.enums.UploadSessionStatus;
import com.studioos.server.shared.media.MediaJobResult;
import com.studioos.server.shared.storage.PresignedUrlService;
import com.studioos.server.shared.storage.StorageObjectMetadata;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BeatServiceTest {

    @Mock
    private BeatRepository beatRepository;
    @Mock
    private BeatGenreRepository beatGenreRepository;
    @Mock
    private StudioRepository studioRepository;
    @Mock
    private UploadSessionRepository uploadSessionRepository;
    @Mock
    private MediaProcessingJobRepository mediaProcessingJobRepository;
    @Mock
    private PresignedUrlService presignedUrlService;
    @Mock
    private com.studioos.server.shared.media.MediaProcessingClient mediaProcessingClient;
    @Mock
    private NotificationServiceImpl notificationService;

    @InjectMocks
    private BeatService beatService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(beatService, "mediaBucket", "studioos-media");
    }

    @Test
    void createDraftAndUploadSessionsRejectsStudioNotOwnedByProducer() {
        CreateBeatRequest request = baseCreateRequest();
        Studio studio = Studio.builder().id("studio-1").ownerId(99).build();

        when(beatGenreRepository.findById("genre-1")).thenReturn(Optional.of(new BeatGenre()));
        when(studioRepository.findById("studio-1")).thenReturn(Optional.of(studio));

        assertThatThrownBy(() -> beatService.createDraftAndUploadSessions(1, request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("does not own this studio");
    }

    @Test
    void createDraftAndUploadSessionsRejectsDuplicateTitleInStudio() {
        CreateBeatRequest request = baseCreateRequest();
        Studio studio = Studio.builder().id("studio-1").ownerId(1).build();

        when(beatGenreRepository.findById("genre-1")).thenReturn(Optional.of(new BeatGenre()));
        when(studioRepository.findById("studio-1")).thenReturn(Optional.of(studio));
        when(beatRepository.existsByStudioIdAndTitleIgnoreCase("studio-1", "Night Ride")).thenReturn(true);

        assertThatThrownBy(() -> beatService.createDraftAndUploadSessions(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void completeUploadValidatesMetadataAndMarksSessionsVerified() {
        Beat beat = Beat.builder()
                .id("beat-1")
                .producerId(1)
                .studioId("studio-1")
                .title("Night Ride")
                .status(BeatStatus.UPLOADING)
                .visibility(BeatVisibility.PUBLIC)
                .build();

        UploadSession audioSession = UploadSession.builder()
                .id("session-a")
                .beatId("beat-1")
                .producerId(1)
                .bucket("studioos-media")
                .objectKey("beats/uploads/beat_beat-1_original.mp3")
                .fileType(UploadFileType.AUDIO)
                .contentType("audio/mpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        UploadSession coverSession = UploadSession.builder()
                .id("session-c")
                .beatId("beat-1")
                .producerId(1)
                .bucket("studioos-media")
                .objectKey("beats/uploads/cover_beat-1_original.jpg")
                .fileType(UploadFileType.COVER)
                .contentType("image/jpeg")
                .status(UploadSessionStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(beatRepository.findById("beat-1")).thenReturn(Optional.of(beat));
        when(beatRepository.save(any(Beat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(uploadSessionRepository.findTopByBeatIdAndFileTypeOrderByCreatedAtDesc("beat-1", UploadFileType.AUDIO))
                .thenReturn(Optional.of(audioSession));
        when(uploadSessionRepository.findTopByBeatIdAndFileTypeOrderByCreatedAtDesc("beat-1", UploadFileType.COVER))
                .thenReturn(Optional.of(coverSession));
        when(presignedUrlService.objectMetadata(anyString(), anyString()))
                .thenReturn(
                        Optional.of(new StorageObjectMetadata(1024L, "audio/mpeg", "\"etag-a\"", Instant.now())),
                        Optional.of(new StorageObjectMetadata(2048L, "image/jpeg", "\"etag-c\"", Instant.now())));
        when(mediaProcessingClient.submitJob(anyString(), anyString(), anyString()))
                .thenReturn("job-1", "job-2", "job-3", "job-4", "job-5", "job-6");
        when(mediaProcessingJobRepository.save(any(MediaProcessingJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BeatUploadCompleteResponse response = beatService.completeUpload(1, "beat-1");

        assertThat(response.getStatus()).isEqualTo(BeatStatus.PROCESSING);
        assertThat(audioSession.getStatus()).isEqualTo(UploadSessionStatus.VERIFIED);
        assertThat(audioSession.getSizeBytes()).isEqualTo(1024L);
        assertThat(audioSession.getChecksum()).isEqualTo("etag-a");
        assertThat(coverSession.getStatus()).isEqualTo(UploadSessionStatus.VERIFIED);
    }

    @Test
    void applyMediaJobResultFinalizesBeatAndStoresThumbnail() {
        Beat beat = Beat.builder()
                .id("beat-1")
                .producerId(1)
                .studioId("studio-1")
                .title("Night Ride")
                .status(BeatStatus.PROCESSING)
                .visibility(BeatVisibility.PUBLIC)
                .build();

        MediaProcessingJob audio = MediaProcessingJob.builder()
                .id("job-a")
                .beatId("beat-1")
                .operation(MediaJobOperation.AUDIO_NORMALIZE)
                .externalJobId("ext-a")
                .status(MediaJobStatus.QUEUED)
                .build();
        MediaProcessingJob preview = MediaProcessingJob.builder()
                .id("job-b")
                .beatId("beat-1")
                .operation(MediaJobOperation.AUDIO_PREVIEW)
                .externalJobId("ext-b")
                .status(MediaJobStatus.SUCCESS)
                .resultReference("s3://bucket/preview.mp3")
                .build();
        MediaProcessingJob waveform = MediaProcessingJob.builder()
                .id("job-c")
                .beatId("beat-1")
                .operation(MediaJobOperation.AUDIO_WAVEFORM)
                .externalJobId("ext-c")
                .status(MediaJobStatus.SUCCESS)
                .resultReference("s3://bucket/waveform.json")
                .build();
        MediaProcessingJob resize = MediaProcessingJob.builder()
                .id("job-d")
                .beatId("beat-1")
                .operation(MediaJobOperation.COVER_RESIZE)
                .externalJobId("ext-d")
                .status(MediaJobStatus.SUCCESS)
                .resultReference("s3://bucket/cover.jpg")
                .build();
        MediaProcessingJob thumbnail = MediaProcessingJob.builder()
                .id("job-e")
                .beatId("beat-1")
                .operation(MediaJobOperation.COVER_THUMBNAIL)
                .externalJobId("ext-e")
                .status(MediaJobStatus.SUCCESS)
                .resultReference("s3://bucket/thumb.jpg")
                .build();
        MediaProcessingJob webp = MediaProcessingJob.builder()
                .id("job-f")
                .beatId("beat-1")
                .operation(MediaJobOperation.COVER_WEBP)
                .externalJobId("ext-f")
                .status(MediaJobStatus.SUCCESS)
                .resultReference("s3://bucket/cover.webp")
                .build();

        when(mediaProcessingJobRepository.findByExternalJobId("ext-a")).thenReturn(Optional.of(audio));
        when(mediaProcessingJobRepository.findByBeatId("beat-1"))
                .thenReturn(List.of(audio, preview, waveform, resize, thumbnail, webp));
        when(mediaProcessingJobRepository.save(any(MediaProcessingJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(beatRepository.findById("beat-1")).thenReturn(Optional.of(beat));
        when(beatRepository.save(any(Beat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean applied = beatService.applyMediaJobResult(MediaJobResult.builder()
                .jobId("ext-a")
                .status(MediaJobStatus.SUCCESS)
                .resultReference("s3://bucket/audio.mp3")
                .build());

        assertThat(applied).isTrue();
        assertThat(beat.getStatus()).isEqualTo(BeatStatus.READY);
        assertThat(beat.getCoverUrl()).isEqualTo("s3://bucket/cover.webp");
        assertThat(beat.getThumbnailUrl()).isEqualTo("s3://bucket/thumb.jpg");
        assertThat(beat.getPreviewUrl()).isEqualTo("s3://bucket/preview.mp3");

        ArgumentCaptor<CreateNotificationRequest> notificationCaptor =
                ArgumentCaptor.forClass(CreateNotificationRequest.class);
        org.mockito.Mockito.verify(notificationService)
                .createNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType())
                .isEqualTo(com.studioos.server.shared.enums.NotificationType.BEAT_PROCESSING_COMPLETED);
    }

    private CreateBeatRequest baseCreateRequest() {
        CreateBeatRequest request = new CreateBeatRequest();
        request.setTitle("Night Ride");
        request.setGenreId("genre-1");
        request.setStudioId("studio-1");
        request.setVisibility(BeatVisibility.PUBLIC);
        request.setBpm(120);
        return request;
    }
}

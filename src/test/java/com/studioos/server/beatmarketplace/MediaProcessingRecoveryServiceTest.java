package com.studioos.server.beatmarketplace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.shared.enums.MediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.media.MediaProcessingClient;

@ExtendWith(MockitoExtension.class)
class MediaProcessingRecoveryServiceTest {

    @Mock
    private MediaProcessingJobRepository jobRepository;

    @Mock
    private MediaProcessingClient mediaProcessingClient;

    @Mock
    private BeatService beatService;

    @InjectMocks
    private MediaProcessingRecoveryService recoveryService;

    @Test
    void marksStuckProcessingJobAsFailedBeforeCheckingMediaHealth() {
        MediaProcessingJob job = MediaProcessingJob.builder()
                .id("job-1")
                .beatId("beat-1")
                .operation(MediaJobOperation.AUDIO_NORMALIZE)
                .status(MediaJobStatus.RUNNING)
                .externalJobId("media-job-1")
                .updatedAt(LocalDateTime.now().minusMinutes(31))
                .build();
        when(jobRepository.findByStatusInAndUpdatedAtBefore(any(), any())).thenReturn(List.of(job));
        when(mediaProcessingClient.health()).thenReturn(false);

        recoveryService.recover();

        org.junit.jupiter.api.Assertions.assertEquals(MediaJobStatus.FAILED, job.getStatus());
        verify(jobRepository).save(job);
        verify(beatService).failBeat("beat-1");
        verify(mediaProcessingClient).health();
        verify(mediaProcessingClient, never()).getJobStatus("media-job-1");
    }
}

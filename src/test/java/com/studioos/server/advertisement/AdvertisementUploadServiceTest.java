package com.studioos.server.advertisement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.AdNotificationService;
import com.studioos.server.advertisement.dto.AdUploadCompleteResponse;
import com.studioos.server.advertisement.pricing.AdvertisementPricingService;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdMediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.enums.UploadSessionStatus;
import com.studioos.server.shared.media.MediaJobResult;
import com.studioos.server.shared.media.MediaProcessingClient;
import com.studioos.server.shared.storage.PresignedUrlService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdvertisementUploadServiceTest {

    @Mock
    private AdCampaignRepository adCampaignRepository;
    @Mock
    private AdBudgetRepository adBudgetRepository;
    @Mock
    private AdvertisementRepository advertisementRepository;
    @Mock
    private AdUploadSessionRepository adUploadSessionRepository;
    @Mock
    private AdMediaProcessingJobRepository adMediaProcessingJobRepository;
    @Mock
    private AdvertisementPricingService advertisementPricingService;
    @Mock
    private PresignedUrlService presignedUrlService;
    @Mock
    private MediaProcessingClient mediaProcessingClient;
    @Mock
    private AdNotificationService adNotificationService;

    @InjectMocks
    private AdvertisementUploadService advertisementUploadService;

    @Test
    void applyMediaJobResultFinalizesAdvertisementToReview() {
        Advertisement ad = Advertisement.builder()
                .id("ad-1")
                .campaignId("campaign-1")
                .status(AdCreativeStatus.PROCESSING)
                .build();

        AdMediaProcessingJob job = AdMediaProcessingJob.builder()
                .id("job-1")
                .advertisementId("ad-1")
                .operation(AdMediaJobOperation.IMAGE_WEBP)
                .externalJobId("ext-1")
                .status(MediaJobStatus.QUEUED)
                .build();

        when(adMediaProcessingJobRepository.findByExternalJobId("ext-1")).thenReturn(Optional.of(job));
        when(adMediaProcessingJobRepository.findByAdvertisementId("ad-1")).thenReturn(List.of(job));
        when(adMediaProcessingJobRepository.save(any(AdMediaProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(advertisementRepository.findById("ad-1")).thenReturn(Optional.of(ad));
        when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaJobResult result = MediaJobResult.builder()
                .jobId("ext-1")
                .status(MediaJobStatus.SUCCESS)
                .resultReference("s3://studioos/ad-1.webp")
                .build();

        boolean handled = advertisementUploadService.applyMediaJobResult(result);

        assertThat(handled).isTrue();
        assertThat(job.getStatus()).isEqualTo(MediaJobStatus.SUCCESS);
        assertThat(job.getResultReference()).isEqualTo("s3://studioos/ad-1.webp");
        assertThat(ad.getStatus()).isEqualTo(AdCreativeStatus.PENDING_REVIEW);
        assertThat(ad.getMediaUrl()).isEqualTo("s3://studioos/ad-1.webp");
        verify(adNotificationService).notifyAdProcessingCompleted(ad);
    }

    @Test
    void applyMediaJobResultFailsAdvertisementOnJobFailure() {
        Advertisement ad = Advertisement.builder()
                .id("ad-1")
                .campaignId("campaign-1")
                .status(AdCreativeStatus.PROCESSING)
                .build();

        AdMediaProcessingJob job = AdMediaProcessingJob.builder()
                .id("job-1")
                .advertisementId("ad-1")
                .operation(AdMediaJobOperation.IMAGE_THUMBNAIL)
                .externalJobId("ext-1")
                .status(MediaJobStatus.QUEUED)
                .build();

        when(adMediaProcessingJobRepository.findByExternalJobId("ext-1")).thenReturn(Optional.of(job));
        when(adMediaProcessingJobRepository.save(any(AdMediaProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(advertisementRepository.findById("ad-1")).thenReturn(Optional.of(ad));
        when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaJobResult result = MediaJobResult.builder()
                .jobId("ext-1")
                .status(MediaJobStatus.FAILED)
                .errorMessage("boom")
                .build();

        boolean handled = advertisementUploadService.applyMediaJobResult(result);

        assertThat(handled).isTrue();
        assertThat(job.getStatus()).isEqualTo(MediaJobStatus.FAILED);
        assertThat(ad.getStatus()).isEqualTo(AdCreativeStatus.FAILED);
        verify(adNotificationService).notifyAdProcessingFailed(ad, "boom");
    }
}

package com.studioos.server.advertisement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.pricing.AdvertisementPricingService;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdCreativeType;
import com.studioos.server.shared.enums.AdMediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.enums.UploadSessionStatus;
import com.studioos.server.shared.media.MediaJobResult;
import com.studioos.server.shared.media.MediaProcessingClient;
import com.studioos.server.shared.storage.PresignedUrlService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdvertisementUploadServiceTest {

  @Mock private AdCampaignRepository adCampaignRepository;
  @Mock private AdBudgetRepository adBudgetRepository;
  @Mock private AdvertisementRepository advertisementRepository;
  @Mock private AdUploadSessionRepository adUploadSessionRepository;
  @Mock private AdMediaProcessingJobRepository adMediaProcessingJobRepository;
  @Mock private AdvertisementPricingService advertisementPricingService;
  @Mock private PresignedUrlService presignedUrlService;
  @Mock private MediaProcessingClient mediaProcessingClient;
  @Mock private AdNotificationService adNotificationService;

  @InjectMocks private AdvertisementUploadService advertisementUploadService;

  @Test
  void completeUploadSubmitsFullyQualifiedS3ReferencesToMediaJobs() {
    ReflectionTestUtils.setField(advertisementUploadService, "mediaBucket", "ads-bucket");
    Advertisement ad = Advertisement.builder()
        .id("ad-1")
        .campaignId("campaign-1")
        .type(AdCreativeType.IMAGE)
        .status(AdCreativeStatus.UPLOADING)
        .build();
    AdUploadSession session = AdUploadSession.builder()
        .advertiserId(22)
        .advertisementId("ad-1")
        .bucket("ads-bucket")
        .objectKey("ads/uploads/ad_ad-1_original")
        .status(UploadSessionStatus.PENDING)
        .build();

    when(advertisementRepository.findById("ad-1")).thenReturn(Optional.of(ad));
    when(adUploadSessionRepository.findByAdvertisementId("ad-1")).thenReturn(List.of(session));
    when(presignedUrlService.objectExists("ads-bucket", "ads/uploads/ad_ad-1_original")).thenReturn(true);
    when(mediaProcessingClient.submitJob(any(), any(), any())).thenAnswer(invocation -> "media-job-" + invocation.getArgument(1));
    when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(invocation -> invocation.getArgument(0));

    advertisementUploadService.completeUpload(22, "ad-1");

    String s3Reference = "s3://ads-bucket/ads/uploads/ad_ad-1_original";
    verify(mediaProcessingClient).submitJob(s3Reference, AdMediaJobOperation.IMAGE_RESIZE.getOperationString(), "{}");
    verify(mediaProcessingClient).submitJob(s3Reference, AdMediaJobOperation.IMAGE_THUMBNAIL.getOperationString(), "{}");
    verify(mediaProcessingClient).submitJob(s3Reference, AdMediaJobOperation.IMAGE_WEBP.getOperationString(), "{}");
    verify(mediaProcessingClient, times(3)).submitJob(eq(s3Reference), any(), eq("{}"));
  }

  @Test
  void applyMediaJobResultFinalizesAdvertisementToReview() {
    Advertisement ad =
        Advertisement.builder()
            .id("ad-1")
            .campaignId("campaign-1")
            .status(AdCreativeStatus.PROCESSING)
            .build();

    AdMediaProcessingJob job =
        AdMediaProcessingJob.builder()
            .id("job-1")
            .advertisementId("ad-1")
            .operation(AdMediaJobOperation.IMAGE_WEBP)
            .externalJobId("ext-1")
            .status(MediaJobStatus.QUEUED)
            .build();

    when(adMediaProcessingJobRepository.findByExternalJobId("ext-1")).thenReturn(Optional.of(job));
    when(adMediaProcessingJobRepository.findByAdvertisementId("ad-1")).thenReturn(List.of(job));
    when(adMediaProcessingJobRepository.save(any(AdMediaProcessingJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(advertisementRepository.findById("ad-1")).thenReturn(Optional.of(ad));
    when(advertisementRepository.save(any(Advertisement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MediaJobResult result =
        MediaJobResult.builder()
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
    Advertisement ad =
        Advertisement.builder()
            .id("ad-1")
            .campaignId("campaign-1")
            .status(AdCreativeStatus.PROCESSING)
            .build();

    AdMediaProcessingJob job =
        AdMediaProcessingJob.builder()
            .id("job-1")
            .advertisementId("ad-1")
            .operation(AdMediaJobOperation.IMAGE_THUMBNAIL)
            .externalJobId("ext-1")
            .status(MediaJobStatus.QUEUED)
            .build();

    when(adMediaProcessingJobRepository.findByExternalJobId("ext-1")).thenReturn(Optional.of(job));
    when(adMediaProcessingJobRepository.save(any(AdMediaProcessingJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(advertisementRepository.findById("ad-1")).thenReturn(Optional.of(ad));
    when(advertisementRepository.save(any(Advertisement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MediaJobResult result =
        MediaJobResult.builder()
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

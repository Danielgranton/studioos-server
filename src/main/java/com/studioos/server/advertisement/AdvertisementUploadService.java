package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.advertisement.campaign.AdBudget;
import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.dto.AdUploadCompleteResponse;
import com.studioos.server.advertisement.dto.AdUploadSessionResponse;
import com.studioos.server.advertisement.dto.CreateAdvertisementRequest;
import com.studioos.server.advertisement.pricing.AdvertisementPriceResult;
import com.studioos.server.advertisement.pricing.AdvertisementPricingService;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdMediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;
import com.studioos.server.shared.enums.UploadSessionStatus;
import com.studioos.server.shared.media.MediaJobResult;
import com.studioos.server.shared.media.MediaProcessingClient;
import com.studioos.server.shared.storage.PresignedUrlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdvertisementUploadService {

    private static final String AD_UPLOAD_PREFIX = "ads/uploads";
    private static final int UPLOAD_URL_EXPIRY_SECONDS = 900;

    private final AdCampaignRepository adCampaignRepository;
    private final AdBudgetRepository adBudgetRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdUploadSessionRepository adUploadSessionRepository;
    private final AdMediaProcessingJobRepository adMediaProcessingJobRepository;
    private final AdvertisementPricingService advertisementPricingService;
    private final PresignedUrlService presignedUrlService;
    private final MediaProcessingClient mediaProcessingClient;
    @Value("${storage.s3.bucket}")
    private String mediaBucket;

    @Transactional
    public AdUploadSessionResponse createAdvertisementAndUploadSession(
            Integer advertiserId, String campaignId, CreateAdvertisementRequest request) {

        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new SecurityException("You do not own this campaign");
        }
        if (campaign.getStatus() != AdCampaignStatus.DRAFT) {
            throw new IllegalStateException("Advertisements can only be added to a DRAFT campaign");
        }

        AdBudget budget = adBudgetRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalStateException("No budget found for campaign " + campaignId));

        Advertisement ad = Advertisement.builder()
                .campaignId(campaignId)
                .type(request.getType())
                .headline(request.getHeadline())
                .description(request.getDescription())
                .ctaText(request.getCtaText())
                .ctaUrl(request.getCtaUrl())
                .status(AdCreativeStatus.UPLOADING)
                .build();

        ad = advertisementRepository.save(ad);

        long durationDays = ChronoUnit.DAYS.between(campaign.getStartDate(), campaign.getEndDate());
        durationDays = Math.max(durationDays, 1);

        AdvertisementPriceResult pricing = advertisementPricingService.priceAndPersist(
                campaignId, ad.getId(), request.getType(), campaign.getPlacement(),
                (int) durationDays, budget.getTotalBudget());

        String objectKey = "%s/ad_%s_original".formatted(AD_UPLOAD_PREFIX, ad.getId());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(UPLOAD_URL_EXPIRY_SECONDS);

        AdUploadSession session = AdUploadSession.builder()
                .advertiserId(advertiserId)
                .advertisementId(ad.getId())
                .bucket(mediaBucket)
                .objectKey(objectKey)
                .contentType(request.getContentType())
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        session = adUploadSessionRepository.save(session);

        String uploadUrl = presignedUrlService.generateUploadUrl(
                mediaBucket, objectKey, request.getContentType(), UPLOAD_URL_EXPIRY_SECONDS);

        return AdUploadSessionResponse.builder()
                .advertisementId(ad.getId())
                .uploadUrl(uploadUrl)
                .uploadSessionId(session.getId())
                .estimatedFinalCpm(pricing.getFinalCpm())
                .estimatedImpressions(pricing.getEstimatedImpressions())
                .build();
    }

    @Transactional
    public AdUploadCompleteResponse completeUpload(Integer advertiserId, String advertisementId) {

        Advertisement ad = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new IllegalArgumentException("Advertisement not found: " + advertisementId));

        if (ad.getStatus() != AdCreativeStatus.UPLOADING) {
            throw new IllegalStateException("Upload already completed or ad is in an unexpected state: " + ad.getStatus());
        }

        List<AdUploadSession> sessions = adUploadSessionRepository.findByAdvertisementId(advertisementId);
        AdUploadSession session = sessions.stream()
                .filter(s -> !s.getAdvertiserId().equals(advertiserId) ? false : true)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing upload session for advertisement " + advertisementId));

        if (!session.getAdvertiserId().equals(advertiserId)) {
            throw new SecurityException("You do not own this upload session");
        }

        boolean exists = presignedUrlService.objectExists(session.getBucket(), session.getObjectKey());
        if (!exists) {
            session.setStatus(UploadSessionStatus.FAILED);
            adUploadSessionRepository.save(session);
            throw new IllegalStateException("Uploaded file not found in storage for advertisement " + advertisementId);
        }

        session.setStatus(UploadSessionStatus.VERIFIED);
        adUploadSessionRepository.save(session);

        ad.setStatus(AdCreativeStatus.PROCESSING);
        ad = advertisementRepository.save(ad);

        submitProcessingJobs(ad, session);

        return AdUploadCompleteResponse.builder()
                .advertisementId(ad.getId())
                .status(ad.getStatus().name())
                .build();
    }

    @Transactional
    public AdUploadSessionResponse refreshUploadSession(Integer advertiserId, String advertisementId) {

        Advertisement ad = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new IllegalArgumentException("Advertisement not found: " + advertisementId));

        if (ad.getStatus() != AdCreativeStatus.UPLOADING) {
            throw new IllegalStateException("Cannot refresh — advertisement is in state: " + ad.getStatus());
        }

        List<AdUploadSession> oldSessions = adUploadSessionRepository.findByAdvertisementId(advertisementId);
        oldSessions.forEach(s -> {
            if (s.getStatus() == UploadSessionStatus.PENDING || s.getStatus() == UploadSessionStatus.FAILED) {
                s.setStatus(UploadSessionStatus.EXPIRED);
            }
        });
        adUploadSessionRepository.saveAll(oldSessions);

        String objectKey = "%s/ad_%s_original".formatted(AD_UPLOAD_PREFIX, ad.getId());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(UPLOAD_URL_EXPIRY_SECONDS);

        String contentType = oldSessions.isEmpty() ?
         null : oldSessions.get(0).getContentType();

        AdUploadSession newSession = AdUploadSession.builder()
                .advertiserId(advertiserId)
                .advertisementId(ad.getId())
                .bucket(mediaBucket)
                .objectKey(objectKey)
                .contentType(contentType)
                .status(UploadSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        newSession = adUploadSessionRepository.save(newSession);

        String uploadUrl = presignedUrlService.generateUploadUrl(
                mediaBucket, objectKey, contentType, UPLOAD_URL_EXPIRY_SECONDS);

        return AdUploadSessionResponse.builder()
                .advertisementId(ad.getId())
                .uploadUrl(uploadUrl)
                .uploadSessionId(newSession.getId())
                .build();
    }

    private void submitProcessingJobs(Advertisement ad, AdUploadSession session) {
        List<AdMediaJobOperation> operations = switch (ad.getType()) {
            case IMAGE -> List.of(AdMediaJobOperation.IMAGE_RESIZE, AdMediaJobOperation.IMAGE_THUMBNAIL, AdMediaJobOperation.IMAGE_WEBP);
            case VIDEO -> List.of(AdMediaJobOperation.VIDEO_COMPRESS, AdMediaJobOperation.VIDEO_THUMBNAIL);
            case AUDIO -> List.of(AdMediaJobOperation.AUDIO_NORMALIZE, AdMediaJobOperation.AUDIO_COMPRESS);
        };

        for (AdMediaJobOperation op : operations) {
            String externalJobId = mediaProcessingClient.submitJob(session.getObjectKey(), op.getOperationString(), "{}");

            AdMediaProcessingJob job = AdMediaProcessingJob.builder()
                    .advertisementId(ad.getId())
                    .operation(op)
                    .externalJobId(externalJobId)
                    .status(MediaJobStatus.QUEUED)
                    .build();

            adMediaProcessingJobRepository.save(job);
        }
    }

    @Transactional
    public boolean applyMediaJobResult(MediaJobResult result) {
        AdMediaProcessingJob job = adMediaProcessingJobRepository.findByExternalJobId(result.getJobId())
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
        adMediaProcessingJobRepository.save(job);

        if (nextStatus == MediaJobStatus.FAILED) {
            failAdvertisement(job.getAdvertisementId(), result.getErrorMessage());
            return true;
        }

        if (nextStatus == MediaJobStatus.SUCCESS) {
            finalizeAdvertisement(job.getAdvertisementId());
        }
        return true;
    }

    private void failAdvertisement(String advertisementId, String errorMessage) {
        advertisementRepository.findById(advertisementId).ifPresent(ad -> {
            if (ad.getStatus() == AdCreativeStatus.READY || ad.getStatus() == AdCreativeStatus.FAILED) {
                return;
            }
            ad.setStatus(AdCreativeStatus.FAILED);
            advertisementRepository.save(ad);
        });
    }

    private void finalizeAdvertisement(String advertisementId) {
        List<AdMediaProcessingJob> jobs = adMediaProcessingJobRepository.findByAdvertisementId(advertisementId);
        if (jobs.stream().anyMatch(job -> job.getStatus() == MediaJobStatus.FAILED)) {
            failAdvertisement(advertisementId, "One or more media jobs failed");
            return;
        }

        boolean allDone = jobs.stream().allMatch(job -> job.getStatus() == MediaJobStatus.SUCCESS);
        if (!allDone) {
            return;
        }

        advertisementRepository.findById(advertisementId).ifPresent(ad -> {
            if (ad.getStatus() != AdCreativeStatus.PROCESSING) {
                return;
            }

            String mediaUrl = ad.getMediaUrl();
            String thumbnailUrl = ad.getThumbnailUrl();

            for (AdMediaProcessingJob job : jobs) {
                String ref = job.getResultReference();
                if (ref == null || ref.isBlank()) {
                    continue;
                }
                switch (job.getOperation()) {
                    case IMAGE_WEBP, VIDEO_COMPRESS, AUDIO_COMPRESS, AUDIO_NORMALIZE -> mediaUrl = ref;
                    case IMAGE_THUMBNAIL, VIDEO_THUMBNAIL -> thumbnailUrl = ref;
                    case IMAGE_RESIZE -> {
                        if (mediaUrl == null || mediaUrl.isBlank()) {
                            mediaUrl = ref;
                        }
                    }
                }
            }

            ad.setMediaUrl(mediaUrl);
            ad.setThumbnailUrl(thumbnailUrl);
            ad.setStatus(AdCreativeStatus.READY);
            advertisementRepository.save(ad);
        });
    }
}

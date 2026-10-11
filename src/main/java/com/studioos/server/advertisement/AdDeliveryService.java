package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.advertisement.campaign.AdBudget;
import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.targeting.TargetingService;
import com.studioos.server.advertisement.dto.AdDeliveryResponse;
import com.studioos.server.advertisement.pricing.AdvertisementPricing;
import com.studioos.server.advertisement.pricing.AdvertisementPricingRepository;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdImpressionVerificationStatus;
import com.studioos.server.shared.enums.AdPlacement;
import com.studioos.server.shared.storage.PresignedUrlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdDeliveryService {

    private static final int SIGNED_URL_EXPIRY_SECONDS = 300;
    private static final int MAX_IMPRESSIONS_PER_HOUR = 3;
    private static final int REPEAT_COOLDOWN_MINUTES = 30;

    private final AdCampaignRepository adCampaignRepository;
    private final AdBudgetRepository adBudgetRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementPricingRepository advertisementPricingRepository;
    private final AdImpressionRepository adImpressionRepository;
    private final AdImpressionVerificationRepository adImpressionVerificationRepository;
    private final PresignedUrlService presignedUrlService;
    private final TargetingService targetingService;

    @Value("${storage.s3.bucket}")
    private String mediaBucket;

    @Transactional
    public Optional<AdDeliveryResponse> selectAdvertisement(AdPlacement placement, Integer userId) {

        LocalDateTime now = LocalDateTime.now();

        List<AdCampaign> candidates = adCampaignRepository.findAll().stream()
                .filter(c -> c.getStatus() == AdCampaignStatus.ACTIVE)
                .filter(c -> c.getPlacement() == placement)
                .filter(c -> !c.getStartDate().isAfter(now) && !c.getEndDate().isBefore(now))
                .filter(c -> targetingService.matchesUser(c.getId(), userId))
                .toList();
        
        List<EligibleAd> eligible = candidates.stream()
                .map(c -> resolveEligibleAd(c, userId, now))
                .flatMap(o -> o.stream())
                .toList();

        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        EligibleAd chosen = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
        String impressionToken = UUID.randomUUID().toString();
        adImpressionVerificationRepository.save(AdImpressionVerification.builder()
                .token(impressionToken)
                .advertisementId(chosen.ad().getId())
                .campaignId(chosen.campaign().getId())
                .userId(userId)
                .placement(placement)
                .status(AdImpressionVerificationStatus.PENDING)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .build());

        return Optional.of(toResponse(chosen, impressionToken));
    }

    private Optional<EligibleAd> resolveEligibleAd(AdCampaign campaign, Integer userId, LocalDateTime now) {
        List<Advertisement> readyAds = advertisementRepository.findByCampaignId(campaign.getId()).stream()
                .filter(a -> a.getStatus() == AdCreativeStatus.READY)
                .filter(a -> userId == null || !isFrequencyCapped(a, userId, now))
                .toList();

        if (readyAds.isEmpty()) {
            return Optional.empty();
        }

        Advertisement ad = readyAds.get(ThreadLocalRandom.current().nextInt(readyAds.size()));

        AdvertisementPricing pricing = advertisementPricingRepository.findByAdvertisementId(ad.getId()).orElse(null);
        if (pricing == null) {
            return Optional.empty(); // shouldn't happen — pricing is created alongside every advertisement
        }

        AdBudget budget = adBudgetRepository.findByCampaignId(campaign.getId()).orElse(null);
        if (budget == null) {
            return Optional.empty();
        }

        int costPerImpression = (int) Math.ceil(pricing.getFinalCpm() / 1000.0);
        if (budget.getRemainingBudget() < costPerImpression) {
            return Optional.empty(); // budget exhausted — excluded entirely, not partially served
        }

        return Optional.of(new EligibleAd(campaign, ad, budget));
    }

    private boolean isFrequencyCapped(Advertisement ad, Integer userId, LocalDateTime now) {
        LocalDateTime hourAgo = now.minusHours(1);
        long impressionsLastHour = adImpressionRepository
                .countByAdvertisementIdAndUserIdAndOccurredAtAfter(ad.getId(), userId, hourAgo);
        if (impressionsLastHour >= MAX_IMPRESSIONS_PER_HOUR) {
            return true;
        }

        LocalDateTime cooldownCutoff = now.minusMinutes(REPEAT_COOLDOWN_MINUTES);
        return adImpressionRepository
                .countByAdvertisementIdAndUserIdAndOccurredAtAfter(ad.getId(), userId, cooldownCutoff) > 0;
    }

    private AdDeliveryResponse toResponse(EligibleAd chosen, String impressionToken) {
        Advertisement ad = chosen.ad();

        String signedMediaUrl = ad.getMediaUrl() != null
                ? presignedUrlService.generateDownloadUrl(mediaBucket, objectKeyForConfiguredBucket(ad.getMediaUrl()), SIGNED_URL_EXPIRY_SECONDS)
                : null;
        String signedThumbnailUrl = ad.getThumbnailUrl() != null
                ? presignedUrlService.generateDownloadUrl(mediaBucket, objectKeyForConfiguredBucket(ad.getThumbnailUrl()), SIGNED_URL_EXPIRY_SECONDS)
                : null;

        return AdDeliveryResponse.builder()
                .advertisementId(ad.getId())
                .impressionToken(impressionToken)
                .type(ad.getType().name())
                .headline(ad.getHeadline())
                .ctaText(ad.getCtaText())
                .ctaUrl(ad.getCtaUrl())
                .mediaUrl(signedMediaUrl)
                .thumbnailUrl(signedThumbnailUrl)
                .build();
    }

    private String objectKeyForConfiguredBucket(String mediaReference) {
        String reference = mediaReference.trim();
        if (reference.startsWith("s3://")) {
            String bucketAndKey = reference.substring("s3://".length());
            int separator = bucketAndKey.indexOf('/');
            if (separator <= 0 || separator == bucketAndKey.length() - 1) {
                throw new IllegalStateException("Ad media reference must include an S3 bucket and object key");
            }
            String referencedBucket = bucketAndKey.substring(0, separator);
            if (!referencedBucket.equals(mediaBucket)) {
                throw new IllegalStateException("Ad media reference bucket does not match configured storage bucket");
            }
            return bucketAndKey.substring(separator + 1);
        }
        if (reference.isBlank()) {
            throw new IllegalStateException("Ad media object key cannot be blank");
        }
        return reference.replaceFirst("^/+", "");
    }

    private record EligibleAd(AdCampaign campaign, Advertisement ad, AdBudget budget) {}
}

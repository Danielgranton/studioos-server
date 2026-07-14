package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.advertisement.campaign.AdBudget;
import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.dto.AdDeliveryResponse;
import com.studioos.server.advertisement.pricing.AdvertisementPricing;
import com.studioos.server.advertisement.pricing.AdvertisementPricingRepository;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdPlacement;
import com.studioos.server.shared.storage.PresignedUrlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdDeliveryService {

    private static final String MEDIA_BUCKET = "studioos-media";
    private static final int SIGNED_URL_EXPIRY_SECONDS = 300;
    private static final int MAX_IMPRESSIONS_PER_HOUR = 3;
    private static final int REPEAT_COOLDOWN_MINUTES = 30;

    private final AdCampaignRepository adCampaignRepository;
    private final AdBudgetRepository adBudgetRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementPricingRepository advertisementPricingRepository;
    private final AdImpressionRepository adImpressionRepository;
    private final PresignedUrlService presignedUrlService;

    @Transactional
    public Optional<AdDeliveryResponse> selectAdvertisement(AdPlacement placement, Integer userId) {

        LocalDateTime now = LocalDateTime.now();

        List<AdCampaign> candidates = adCampaignRepository.findAll().stream()
                .filter(c -> c.getStatus() == AdCampaignStatus.ACTIVE)
                .filter(c -> c.getPlacement() == placement)
                .filter(c -> !c.getStartDate().isAfter(now) && !c.getEndDate().isBefore(now))
                .toList();
        
        List<EligibleAd> eligible = candidates.stream()
                .map(c -> resolveEligibleAd(c, userId, now))
                .flatMap(Optional::stream)
                .toList();

        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        EligibleAd chosen = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));

        chargeImpression(chosen);
        recordImpression(chosen, placement, userId);

        return Optional.of(toResponse(chosen));
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

        double costPerImpression = pricing.getFinalCpm() / 1000.0;
        if (budget.getRemainingBudget() < costPerImpression) {
            return Optional.empty(); // budget exhausted — excluded entirely, not partially served
        }

        return Optional.of(new EligibleAd(campaign, ad, budget, costPerImpression));
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

    private void chargeImpression(EligibleAd chosen) {
        AdBudget budget = chosen.budget();
        int cost = (int) Math.ceil(chosen.costPerImpression());
        budget.setRemainingBudget(budget.getRemainingBudget() - cost);
        budget.setSpentBudget(budget.getSpentBudget() + cost);
        adBudgetRepository.save(budget);
    }

    private void recordImpression(EligibleAd chosen, AdPlacement placement, Integer userId) {
        AdImpression impression = AdImpression.builder()
                .advertisementId(chosen.ad().getId())
                .campaignId(chosen.campaign().getId())
                .userId(userId)
                .placement(placement)
                .build();
        adImpressionRepository.save(impression);

        chosen.ad(); // no-op, kept for clarity that ad itself isn't mutated here
    }

    private AdDeliveryResponse toResponse(EligibleAd chosen) {
        Advertisement ad = chosen.ad();

        String signedMediaUrl = ad.getMediaUrl() != null
                ? presignedUrlService.generateDownloadUrl(MEDIA_BUCKET, ad.getMediaUrl(), SIGNED_URL_EXPIRY_SECONDS)
                : null;
        String signedThumbnailUrl = ad.getThumbnailUrl() != null
                ? presignedUrlService.generateDownloadUrl(MEDIA_BUCKET, ad.getThumbnailUrl(), SIGNED_URL_EXPIRY_SECONDS)
                : null;

        return AdDeliveryResponse.builder()
                .advertisementId(ad.getId())
                .type(ad.getType().name())
                .headline(ad.getHeadline())
                .ctaText(ad.getCtaText())
                .ctaUrl(ad.getCtaUrl())
                .mediaUrl(signedMediaUrl)
                .thumbnailUrl(signedThumbnailUrl)
                .build();
    }

    private record EligibleAd(AdCampaign campaign, Advertisement ad, AdBudget budget, double costPerImpression) {}
}

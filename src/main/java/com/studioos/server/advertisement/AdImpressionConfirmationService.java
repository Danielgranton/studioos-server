package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.dto.AdImpressionConfirmationResponse;
import com.studioos.server.advertisement.pricing.AdvertisementPricingRepository;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdImpressionVerificationStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdImpressionConfirmationService {

    private final AdImpressionVerificationRepository verificationRepository;
    private final AdCampaignRepository campaignRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementPricingRepository pricingRepository;
    private final AdBudgetRepository budgetRepository;
    private final AdImpressionRepository impressionRepository;

    @Transactional
    public AdImpressionConfirmationResponse confirm(String token, Integer userId) {
        AdImpressionVerification verification = verificationRepository.findByTokenForUpdate(token).orElse(null);
        if (verification == null || !Objects.equals(verification.getUserId(), userId)) {
            return response(false);
        }
        if (verification.getStatus() == AdImpressionVerificationStatus.CONFIRMED) {
            return response(true);
        }
        if (verification.getStatus() != AdImpressionVerificationStatus.PENDING
                || !verification.getExpiresAt().isAfter(LocalDateTime.now())) {
            expire(verification);
            return response(false);
        }

        LocalDateTime now = LocalDateTime.now();
        var campaign = campaignRepository.findByIdForUpdate(verification.getCampaignId()).orElse(null);
        var advertisement = advertisementRepository.findByIdForUpdate(verification.getAdvertisementId()).orElse(null);
        if (campaign == null || advertisement == null
                || campaign.getStatus() != AdCampaignStatus.ACTIVE
                || campaign.getPaymentStatus() != AdPaymentStatus.PAID
                || campaign.getPlacement() != verification.getPlacement()
                || campaign.getStartDate().isAfter(now)
                || campaign.getEndDate().isBefore(now)
                || advertisement.getStatus() != AdCreativeStatus.READY
                || !advertisement.getCampaignId().equals(campaign.getId())) {
            expire(verification);
            return response(false);
        }

        var pricing = pricingRepository.findByAdvertisementId(advertisement.getId()).orElse(null);
        if (pricing == null || pricing.getFinalCpm() == null || pricing.getFinalCpm() <= 0) {
            expire(verification);
            return response(false);
        }
        int cost = (int) Math.ceil(pricing.getFinalCpm() / 1000.0);
        int updatedBudgets = budgetRepository.debitForVerifiedImpression(campaign.getId(), cost);
        if (updatedBudgets != 1) {
            expire(verification);
            return response(false);
        }

        impressionRepository.save(AdImpression.builder()
                .advertisementId(advertisement.getId())
                .campaignId(campaign.getId())
                .userId(userId)
                .placement(verification.getPlacement())
                .build());
        verification.setStatus(AdImpressionVerificationStatus.CONFIRMED);
        verification.setConfirmedAt(now);
        verificationRepository.save(verification);
        return response(true);
    }

    @Scheduled(fixedDelayString = "${ads.impression-token-cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupOldVerifications() {
        LocalDateTime now = LocalDateTime.now();
        int expiredRemoved = verificationRepository.deleteExpired(
                AdImpressionVerificationStatus.PENDING, now.minusHours(24));
        int terminalRemoved = verificationRepository.deleteOldTerminal(
                java.util.List.of(AdImpressionVerificationStatus.CONFIRMED, AdImpressionVerificationStatus.EXPIRED),
                now.minusDays(90));
        if (expiredRemoved + terminalRemoved > 0) {
            log.debug("Removed {} expired and {} old ad impression verification tokens", expiredRemoved, terminalRemoved);
        }
    }

    private void expire(AdImpressionVerification verification) {
        verification.setStatus(AdImpressionVerificationStatus.EXPIRED);
        verificationRepository.save(verification);
    }

    private AdImpressionConfirmationResponse response(boolean recorded) {
        return AdImpressionConfirmationResponse.builder().recorded(recorded).build();
    }
}

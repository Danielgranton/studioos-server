package com.studioos.server.advertisement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.pricing.AdvertisementPricing;
import com.studioos.server.advertisement.pricing.AdvertisementPricingRepository;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdCreativeType;
import com.studioos.server.shared.enums.AdImpressionVerificationStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;
import com.studioos.server.shared.enums.AdPlacement;

@ExtendWith(MockitoExtension.class)
class AdImpressionConfirmationServiceTest {

    @Mock private AdImpressionVerificationRepository verificationRepository;
    @Mock private AdCampaignRepository campaignRepository;
    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private AdvertisementPricingRepository pricingRepository;
    @Mock private AdBudgetRepository budgetRepository;
    @Mock private AdImpressionRepository impressionRepository;

    @InjectMocks private AdImpressionConfirmationService service;

    @Test
    void confirmsAndBillsAViewableImpressionOnce() {
        AdImpressionVerification verification = verification(AdImpressionVerificationStatus.PENDING);
        when(verificationRepository.findByTokenForUpdate("token-1")).thenReturn(Optional.of(verification));
        when(campaignRepository.findByIdForUpdate("campaign-1")).thenReturn(Optional.of(campaign()));
        when(advertisementRepository.findByIdForUpdate("ad-1")).thenReturn(Optional.of(advertisement()));
        when(pricingRepository.findByAdvertisementId("ad-1"))
                .thenReturn(Optional.of(AdvertisementPricing.builder().finalCpm(100.0).build()));
        when(budgetRepository.debitForVerifiedImpression("campaign-1", 1)).thenReturn(1);

        var result = service.confirm("token-1", 7);

        assertThat(result.isRecorded()).isTrue();
        assertThat(verification.getStatus()).isEqualTo(AdImpressionVerificationStatus.CONFIRMED);
        verify(budgetRepository).debitForVerifiedImpression("campaign-1", 1);
        verify(impressionRepository).save(any(AdImpression.class));
    }

    @Test
    void repeatedConfirmationIsIdempotent() {
        AdImpressionVerification verification = verification(AdImpressionVerificationStatus.CONFIRMED);
        when(verificationRepository.findByTokenForUpdate("token-1")).thenReturn(Optional.of(verification));

        var result = service.confirm("token-1", 7);

        assertThat(result.isRecorded()).isTrue();
        verify(budgetRepository, never()).debitForVerifiedImpression(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(impressionRepository, never()).save(any(AdImpression.class));
    }

    @Test
    void budgetRaceRejectsImpressionWithoutWritingAnalytics() {
        AdImpressionVerification verification = verification(AdImpressionVerificationStatus.PENDING);
        when(verificationRepository.findByTokenForUpdate("token-1")).thenReturn(Optional.of(verification));
        when(campaignRepository.findByIdForUpdate("campaign-1")).thenReturn(Optional.of(campaign()));
        when(advertisementRepository.findByIdForUpdate("ad-1")).thenReturn(Optional.of(advertisement()));
        when(pricingRepository.findByAdvertisementId("ad-1"))
                .thenReturn(Optional.of(AdvertisementPricing.builder().finalCpm(100.0).build()));
        when(budgetRepository.debitForVerifiedImpression("campaign-1", 1)).thenReturn(0);

        var result = service.confirm("token-1", 7);

        assertThat(result.isRecorded()).isFalse();
        assertThat(verification.getStatus()).isEqualTo(AdImpressionVerificationStatus.EXPIRED);
        verify(impressionRepository, never()).save(any(AdImpression.class));
    }

    private AdImpressionVerification verification(AdImpressionVerificationStatus status) {
        return AdImpressionVerification.builder()
                .token("token-1")
                .advertisementId("ad-1")
                .campaignId("campaign-1")
                .userId(7)
                .placement(AdPlacement.EXPLORE_PAGE)
                .status(status)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    private AdCampaign campaign() {
        LocalDateTime now = LocalDateTime.now();
        return AdCampaign.builder()
                .id("campaign-1")
                .status(AdCampaignStatus.ACTIVE)
                .paymentStatus(AdPaymentStatus.PAID)
                .placement(AdPlacement.EXPLORE_PAGE)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(1))
                .build();
    }

    private Advertisement advertisement() {
        return Advertisement.builder()
                .id("ad-1")
                .campaignId("campaign-1")
                .type(AdCreativeType.IMAGE)
                .status(AdCreativeStatus.READY)
                .build();
    }
}

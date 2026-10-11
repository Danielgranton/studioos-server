package com.studioos.server.advertisement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.studioos.server.advertisement.campaign.AdBudget;
import com.studioos.server.advertisement.campaign.AdBudgetRepository;
import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.pricing.AdvertisementPricing;
import com.studioos.server.advertisement.pricing.AdvertisementPricingRepository;
import com.studioos.server.advertisement.targeting.TargetingService;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdCreativeType;
import com.studioos.server.shared.enums.AdPaymentStatus;
import com.studioos.server.shared.enums.AdPlacement;
import com.studioos.server.shared.storage.PresignedUrlService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdDeliveryServiceTest {

    @Mock
    private AdCampaignRepository adCampaignRepository;
    @Mock
    private AdBudgetRepository adBudgetRepository;
    @Mock
    private AdvertisementRepository advertisementRepository;
    @Mock
    private AdvertisementPricingRepository advertisementPricingRepository;
    @Mock
    private AdImpressionRepository adImpressionRepository;
    @Mock
    private AdImpressionVerificationRepository adImpressionVerificationRepository;
    @Mock
    private PresignedUrlService presignedUrlService;
    @Mock
    private TargetingService targetingService;

    @InjectMocks
    private AdDeliveryService adDeliveryService;

    @Test
    void skipsAdsThatAreWithinCooldownForSameUser() {
        AdCampaign campaign = AdCampaign.builder()
                .id("campaign-1")
                .status(AdCampaignStatus.ACTIVE)
                .paymentStatus(AdPaymentStatus.PAID)
                .placement(AdPlacement.HOME_BANNER)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        Advertisement capped = Advertisement.builder()
                .id("ad-capped")
                .campaignId("campaign-1")
                .type(AdCreativeType.IMAGE)
                .headline("Capped")
                .ctaUrl("https://example.com/capped")
                .status(AdCreativeStatus.READY)
                .build();

        Advertisement eligible = Advertisement.builder()
                .id("ad-eligible")
                .campaignId("campaign-1")
                .type(AdCreativeType.IMAGE)
                .headline("Eligible")
                .mediaUrl("s3://configured-studioos-bucket/ads/uploads/ad-eligible/creative.webp")
                .thumbnailUrl("s3://configured-studioos-bucket/ads/uploads/ad-eligible/thumbnail.webp")
                .ctaUrl("https://example.com/eligible")
                .status(AdCreativeStatus.READY)
                .build();

        AdBudget budget = AdBudget.builder()
                .campaignId("campaign-1")
                .totalBudget(1000)
                .remainingBudget(1000)
                .spentBudget(0)
                .build();

        AdvertisementPricing pricing = AdvertisementPricing.builder()
                .advertisementId("ad-eligible")
                .finalCpm(100.0)
                .build();

        when(adCampaignRepository.findAll()).thenReturn(List.of(campaign));
        when(advertisementRepository.findByCampaignId("campaign-1")).thenReturn(List.of(capped, eligible));
        when(adBudgetRepository.findByCampaignId("campaign-1")).thenReturn(Optional.of(budget));
        when(advertisementPricingRepository.findByAdvertisementId("ad-eligible")).thenReturn(Optional.of(pricing));
        ReflectionTestUtils.setField(adDeliveryService, "mediaBucket", "configured-studioos-bucket");
        when(presignedUrlService.generateDownloadUrl("configured-studioos-bucket", "ads/uploads/ad-eligible/creative.webp", 300))
                .thenReturn("https://storage.example/creative");
        when(presignedUrlService.generateDownloadUrl("configured-studioos-bucket", "ads/uploads/ad-eligible/thumbnail.webp", 300))
                .thenReturn("https://storage.example/thumbnail");
        when(targetingService.matchesUser("campaign-1", 7)).thenReturn(true);
        when(adImpressionRepository.countByAdvertisementIdAndUserIdAndOccurredAtAfter(eq("ad-capped"), eq(7), any()))
                .thenReturn(3L, 1L);
        when(adImpressionRepository.countByAdvertisementIdAndUserIdAndOccurredAtAfter(eq("ad-eligible"), eq(7), any()))
                .thenReturn(0L, 0L);

        assertThat(adDeliveryService.selectAdvertisement(AdPlacement.HOME_BANNER, 7))
                .isPresent()
                .hasValueSatisfying(resp -> {
                    assertThat(resp.getAdvertisementId()).isEqualTo("ad-eligible");
                    assertThat(resp.getImpressionToken()).isNotBlank();
                    assertThat(resp.getMediaUrl()).isEqualTo("https://storage.example/creative");
                    assertThat(resp.getThumbnailUrl()).isEqualTo("https://storage.example/thumbnail");
                });

        verify(presignedUrlService).generateDownloadUrl("configured-studioos-bucket", "ads/uploads/ad-eligible/creative.webp", 300);
        verify(presignedUrlService).generateDownloadUrl("configured-studioos-bucket", "ads/uploads/ad-eligible/thumbnail.webp", 300);
        verify(adImpressionVerificationRepository).save(any(AdImpressionVerification.class));
        verify(adImpressionRepository, never()).save(any(AdImpression.class));
        assertThat(budget.getRemainingBudget()).isEqualTo(1000);
        assertThat(budget.getSpentBudget()).isEqualTo(0);
    }
}

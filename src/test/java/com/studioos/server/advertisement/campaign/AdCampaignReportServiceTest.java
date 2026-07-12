package com.studioos.server.advertisement.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.studioos.server.advertisement.AdClickRepository;
import com.studioos.server.advertisement.AdImpressionRepository;
import com.studioos.server.advertisement.AdvertisementRepository;
import com.studioos.server.advertisement.campaign.dto.AdCampaignReportResponse;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;
import com.studioos.server.shared.enums.AdPlacement;
import com.studioos.server.shared.enums.Role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdCampaignReportServiceTest {

    @Mock
    private AdCampaignRepository adCampaignRepository;
    @Mock
    private AdBudgetRepository adBudgetRepository;
    @Mock
    private AdvertisementRepository advertisementRepository;
    @Mock
    private AdImpressionRepository adImpressionRepository;
    @Mock
    private AdClickRepository adClickRepository;

    @InjectMocks
    private AdCampaignReportService reportService;

    @Test
    void returnsCampaignSummaryForOwner() {
        AdCampaign campaign = AdCampaign.builder()
                .id("campaign-1")
                .advertiserId(99)
                .title("Summer push")
                .placement(AdPlacement.HOME_BANNER)
                .status(AdCampaignStatus.ACTIVE)
                .paymentStatus(AdPaymentStatus.PAID)
                .build();

        AdBudget budget = AdBudget.builder()
                .campaignId("campaign-1")
                .totalBudget(10000)
                .spentBudget(2500)
                .remainingBudget(7500)
                .build();

        when(adCampaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
        when(adBudgetRepository.findByCampaignId("campaign-1")).thenReturn(Optional.of(budget));
        when(adImpressionRepository.countByCampaignId("campaign-1")).thenReturn(100L);
        when(adClickRepository.countByCampaignId("campaign-1")).thenReturn(5L);
        when(advertisementRepository.countByCampaignId("campaign-1")).thenReturn(2L);

        AdCampaignReportResponse response = reportService.getReport(99, "campaign-1");

        assertThat(response.getCampaignId()).isEqualTo("campaign-1");
        assertThat(response.getImpressions()).isEqualTo(100L);
        assertThat(response.getClicks()).isEqualTo(5L);
        assertThat(response.getClickThroughRate()).isEqualTo(0.05);
        assertThat(response.getSpentBudget()).isEqualTo(2500);
    }
}

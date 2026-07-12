package com.studioos.server.advertisement.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.studioos.server.advertisement.AdNotificationService;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdCampaignActivationSchedulerTest {

    @Mock
    private AdCampaignRepository adCampaignRepository;
    @Mock
    private AdNotificationService adNotificationService;

    @InjectMocks
    private AdCampaignActivationScheduler scheduler;

    @Test
    void activatesPaidScheduledCampaignsWithinWindow() {
        AdCampaign campaign = AdCampaign.builder()
                .id("campaign-1")
                .status(AdCampaignStatus.SCHEDULED)
                .paymentStatus(AdPaymentStatus.PAID)
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusHours(1))
                .build();

        when(adCampaignRepository.findByStatusAndPaymentStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(), any(), any(), any()))
                .thenReturn(List.of(campaign));
        when(adCampaignRepository.save(any(AdCampaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.activateDueCampaigns();

        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.ACTIVE);
    }
}

package com.studioos.server.advertisement.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.advertisement.AdNotificationService;
import com.studioos.server.advertisement.campaign.dto.CampaignPaymentInitiationResponse;
import com.studioos.server.advertisement.campaign.dto.CreateCampaignRequest;
import com.studioos.server.advertisement.campaign.dto.InitiateCampaignPaymentRequest;
import com.studioos.server.payment.PaymentService;
import com.studioos.server.payment.Transaction;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;
import com.studioos.server.shared.enums.AdPlacement;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.events.TransactionResolvedEvent;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class AdCampaignServiceTest {

    @Mock private AdCampaignRepository adCampaignRepository;
    @Mock private AdBudgetRepository adBudgetRepository;
    @Mock private PaymentService paymentService;
    @Mock private AdNotificationService adNotificationService;

    @InjectMocks private AdCampaignService adCampaignService;

    @Test
    void createCampaignPersistsCampaignAndBudget() {
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setStudioId("studio-1");
        request.setTitle("Launch");
        request.setPlacement(AdPlacement.HOME_BANNER);
        request.setStartDate(LocalDateTime.now().plusDays(1));
        request.setEndDate(LocalDateTime.now().plusDays(10));
        request.setTotalBudget(10000);
        request.setDailyBudget(1000);

        when(adCampaignRepository.save(any(AdCampaign.class))).thenAnswer(invocation -> {
            AdCampaign campaign = invocation.getArgument(0);
            campaign.setId("campaign-1");
            return campaign;
        });
        when(adBudgetRepository.save(any(AdBudget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adCampaignService.createCampaign(42, request);

        ArgumentCaptor<AdCampaign> campaignCaptor = ArgumentCaptor.forClass(AdCampaign.class);
        verify(adCampaignRepository).save(campaignCaptor.capture());
        assertThat(campaignCaptor.getValue().getStatus()).isEqualTo(AdCampaignStatus.DRAFT);
        assertThat(campaignCaptor.getValue().getPaymentStatus()).isEqualTo(AdPaymentStatus.PENDING);
    }

    @Test
    void initiatePaymentStartsPaymentAndStoresTransactionId() {
        AdCampaign campaign = AdCampaign.builder()
                .id("campaign-1")
                .advertiserId(7)
                .studioId("studio-1")
                .title("Launch")
                .status(AdCampaignStatus.DRAFT)
                .paymentStatus(AdPaymentStatus.PENDING)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();
        AdBudget budget = AdBudget.builder()
                .campaignId("campaign-1")
                .totalBudget(10000)
                .remainingBudget(10000)
                .build();
        Transaction transaction = Transaction.builder()
                .id("tx-1")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.AD_CAMPAIGN)
                .build();

        when(adCampaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
        when(adBudgetRepository.findByCampaignId("campaign-1")).thenReturn(Optional.of(budget));
        when(paymentService.initiateAdCampaignPayment(7, "studio-1", 10000, "+254700000000", "Ad campaign payment: Launch"))
                .thenReturn(transaction);
        when(adCampaignRepository.save(any(AdCampaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InitiateCampaignPaymentRequest request = new InitiateCampaignPaymentRequest();
        request.setPhoneNumber("+254700000000");

        CampaignPaymentInitiationResponse response = adCampaignService.initiatePayment(7, "campaign-1", request);

        assertThat(response.getTransactionId()).isEqualTo("tx-1");
        assertThat(campaign.getTransactionId()).isEqualTo("tx-1");
    }

    @Test
    void transactionResolvedActivatesCampaignWhenPaid() {
        AdCampaign campaign = AdCampaign.builder()
                .id("campaign-1")
                .advertiserId(7)
                .studioId("studio-1")
                .title("Launch")
                .status(AdCampaignStatus.DRAFT)
                .paymentStatus(AdPaymentStatus.PENDING)
                .transactionId("tx-1")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        when(adCampaignRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(campaign));
        when(adCampaignRepository.save(any(AdCampaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adCampaignService.onTransactionResolved(TransactionResolvedEvent.builder()
                .transactionId("tx-1")
                .type(TransactionType.AD_CAMPAIGN)
                .success(true)
                .build());

        assertThat(campaign.getPaymentStatus()).isEqualTo(AdPaymentStatus.PAID);
        assertThat(campaign.getStatus()).isEqualTo(AdCampaignStatus.ACTIVE);
        verify(adNotificationService).notifyCampaignPaymentResult(campaign, true);
    }

    @Test
    void transactionResolvedMarksCampaignFailedOnPaymentFailure() {
        AdCampaign campaign = AdCampaign.builder()
                .id("campaign-1")
                .advertiserId(7)
                .studioId("studio-1")
                .title("Launch")
                .status(AdCampaignStatus.DRAFT)
                .paymentStatus(AdPaymentStatus.PENDING)
                .transactionId("tx-1")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        when(adCampaignRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(campaign));
        when(adCampaignRepository.save(any(AdCampaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adCampaignService.onTransactionResolved(TransactionResolvedEvent.builder()
                .transactionId("tx-1")
                .type(TransactionType.AD_CAMPAIGN)
                .success(false)
                .build());

        assertThat(campaign.getPaymentStatus()).isEqualTo(AdPaymentStatus.FAILED);
        verify(adNotificationService).notifyCampaignPaymentResult(campaign, false);
    }

    @Test
    void pausesOwnedScheduledCampaign() {
        AdCampaign campaign = campaign(7, AdCampaignStatus.SCHEDULED, AdPaymentStatus.PAID);
        when(adCampaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
        when(adBudgetRepository.findByCampaignId("campaign-1")).thenReturn(Optional.of(budget()));
        when(adCampaignRepository.save(campaign)).thenReturn(campaign);

        var response = adCampaignService.pauseCampaign(7, "campaign-1");

        assertThat(response.getStatus()).isEqualTo(AdCampaignStatus.PAUSED);
        verify(adCampaignRepository).save(campaign);
    }

    @Test
    void rejectsCampaignManagementByAnotherProducer() {
        when(adCampaignRepository.findById("campaign-1"))
                .thenReturn(Optional.of(campaign(8, AdCampaignStatus.ACTIVE, AdPaymentStatus.PAID)));

        assertThatThrownBy(() -> adCampaignService.pauseCampaign(7, "campaign-1"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void cannotResumeCampaignBeforePaymentIsComplete() {
        when(adCampaignRepository.findById("campaign-1"))
                .thenReturn(Optional.of(campaign(7, AdCampaignStatus.PAUSED, AdPaymentStatus.PENDING)));

        assertThatThrownBy(() -> adCampaignService.resumeCampaign(7, "campaign-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payment must be complete");
    }

    private AdCampaign campaign(Integer ownerId, AdCampaignStatus status, AdPaymentStatus paymentStatus) {
        return AdCampaign.builder()
                .id("campaign-1")
                .advertiserId(ownerId)
                .title("Campaign")
                .status(status)
                .paymentStatus(paymentStatus)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .build();
    }

    private AdBudget budget() {
        return AdBudget.builder()
                .campaignId("campaign-1")
                .totalBudget(5000)
                .remainingBudget(5000)
                .spentBudget(0)
                .build();
    }
}

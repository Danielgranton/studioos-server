package com.studioos.server.advertisement.campaign;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.studioos.server.advertisement.AdNotificationService;
import com.studioos.server.advertisement.campaign.dto.CampaignPaymentInitiationResponse;
import com.studioos.server.advertisement.campaign.dto.AdCampaignManagementResponse;
import com.studioos.server.advertisement.campaign.dto.CampaignResponse;
import com.studioos.server.advertisement.campaign.dto.CreateCampaignRequest;
import com.studioos.server.advertisement.campaign.dto.InitiateCampaignPaymentRequest;
import com.studioos.server.payment.PaymentService;
import com.studioos.server.payment.Transaction;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.events.TransactionResolvedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdCampaignService {

    private final AdCampaignRepository adCampaignRepository;
    private final AdBudgetRepository adBudgetRepository;
    private final PaymentService paymentService;
    private final AdNotificationService adNotificationService;

    @Transactional(readOnly = true)
    public Page<AdCampaignManagementResponse> getMyCampaigns(Integer advertiserId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        return adCampaignRepository.findByAdvertiserId(advertiserId,
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toManagementResponse);
    }

    @Transactional(readOnly = true)
    public AdCampaignManagementResponse getCampaign(Integer advertiserId, String campaignId) {
        return toManagementResponse(requireOwnedCampaign(advertiserId, campaignId));
    }

    @Transactional
    public AdCampaignManagementResponse pauseCampaign(Integer advertiserId, String campaignId) {
        AdCampaign campaign = requireOwnedCampaign(advertiserId, campaignId);
        if (campaign.getStatus() != AdCampaignStatus.ACTIVE && campaign.getStatus() != AdCampaignStatus.SCHEDULED) {
            throw new IllegalStateException("Only active or scheduled campaigns can be paused");
        }
        campaign.setStatus(AdCampaignStatus.PAUSED);
        return toManagementResponse(adCampaignRepository.save(campaign));
    }

    @Transactional
    public AdCampaignManagementResponse resumeCampaign(Integer advertiserId, String campaignId) {
        AdCampaign campaign = requireOwnedCampaign(advertiserId, campaignId);
        if (campaign.getStatus() != AdCampaignStatus.PAUSED) {
            throw new IllegalStateException("Only paused campaigns can be resumed");
        }
        if (campaign.getPaymentStatus() != AdPaymentStatus.PAID) {
            throw new IllegalStateException("Campaign payment must be complete before resuming");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!campaign.getEndDate().isAfter(now)) {
            campaign.setStatus(AdCampaignStatus.COMPLETED);
        } else {
            campaign.setStatus(campaign.getStartDate().isAfter(now)
                    ? AdCampaignStatus.SCHEDULED
                    : AdCampaignStatus.ACTIVE);
        }
        return toManagementResponse(adCampaignRepository.save(campaign));
    }

    private AdCampaign requireOwnedCampaign(Integer advertiserId, String campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));
        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new SecurityException("You do not own this campaign");
        }
        return campaign;
    }

    private AdCampaignManagementResponse toManagementResponse(AdCampaign campaign) {
        AdBudget budget = adBudgetRepository.findByCampaignId(campaign.getId())
                .orElseThrow(() -> new IllegalStateException("Campaign budget is missing: " + campaign.getId()));
        return AdCampaignManagementResponse.builder()
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .description(campaign.getDescription())
                .placement(campaign.getPlacement())
                .status(campaign.getStatus())
                .paymentStatus(campaign.getPaymentStatus())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .totalBudget(budget.getTotalBudget())
                .spentBudget(budget.getSpentBudget())
                .remainingBudget(budget.getRemainingBudget())
                .createdAt(campaign.getCreatedAt())
                .build();
    }

    @Transactional
    public CampaignResponse createCampaign(Integer advertiserId, CreateCampaignRequest request) {

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        AdCampaign campaign = AdCampaign.builder()
                .advertiserId(advertiserId)
                .studioId(request.getStudioId())
                .title(request.getTitle())
                .description(request.getDescription())
                .placement(request.getPlacement())
                .status(AdCampaignStatus.DRAFT)
                .paymentStatus(AdPaymentStatus.PENDING)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        campaign = adCampaignRepository.save(campaign);

        AdBudget budget = AdBudget.builder()
                .campaignId(campaign.getId())
                .totalBudget(request.getTotalBudget())
                .dailyBudget(request.getDailyBudget())
                .remainingBudget(request.getTotalBudget())
                .build();

        adBudgetRepository.save(budget);

        return CampaignResponse.builder()
                .campaignId(campaign.getId())
                .status(campaign.getStatus().name())
                .build();
    }

    @Transactional
    public CampaignPaymentInitiationResponse initiatePayment(
            Integer advertiserId, String campaignId, InitiateCampaignPaymentRequest request) {

        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw new SecurityException("You do not own this campaign");
        }

        if (campaign.getPaymentStatus() != AdPaymentStatus.PENDING
                && campaign.getPaymentStatus() != AdPaymentStatus.FAILED) {
            throw new IllegalStateException("Campaign payment already resolved: " + campaign.getPaymentStatus());
        }

        AdBudget budget = adBudgetRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new IllegalStateException("No budget found for campaign " + campaignId));

        Transaction transaction = paymentService.initiateAdCampaignPayment(
                advertiserId, campaign.getStudioId(), budget.getTotalBudget(),
                request.getPhoneNumber(), "Ad campaign payment: " + campaign.getTitle());

        campaign.setTransactionId(transaction.getId());
        adCampaignRepository.save(campaign);

        return CampaignPaymentInitiationResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus().name())
                .build();
    }

    @EventListener
    @Transactional
    public void onTransactionResolved(TransactionResolvedEvent event) {

        if (event.getType() != TransactionType.AD_CAMPAIGN) {
            return;
        }

        AdCampaign campaign = adCampaignRepository.findByTransactionId(event.getTransactionId()).orElse(null);
        if (campaign == null) {
            log.warn("No AdCampaign found for resolved transaction {}", event.getTransactionId());
            return;
        }

        if (campaign.getPaymentStatus() == AdPaymentStatus.PAID) {
            log.warn("Ignoring duplicate transaction-resolved event for campaign {} (already PAID)", campaign.getId());
            return;
        }

        if (!event.isSuccess()) {
            campaign.setPaymentStatus(AdPaymentStatus.FAILED);
            adCampaignRepository.save(campaign);
            adNotificationService.notifyCampaignPaymentResult(campaign, false);
            return;
        }

        campaign.setPaymentStatus(AdPaymentStatus.PAID);

        LocalDateTime now = LocalDateTime.now();
        campaign.setStatus(
                !campaign.getStartDate().isAfter(now) && !campaign.getEndDate().isBefore(now)
                        ? AdCampaignStatus.ACTIVE
                        : AdCampaignStatus.SCHEDULED
        );

        adCampaignRepository.save(campaign);
        adNotificationService.notifyCampaignPaymentResult(campaign, true);
    }
}

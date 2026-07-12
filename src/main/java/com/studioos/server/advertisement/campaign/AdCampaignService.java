package com.studioos.server.advertisement.campaign;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studioos.server.advertisement.campaign.dto.CampaignResponse;
import com.studioos.server.advertisement.campaign.dto.CreateCampaignRequest;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdCampaignService {

    private final AdCampaignRepository adCampaignRepository;
    private final AdBudgetRepository adBudgetRepository;

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
}
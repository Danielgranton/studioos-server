package com.studioos.server.advertisement.campaign;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.advertisement.AdClickRepository;
import com.studioos.server.advertisement.AdImpressionRepository;
import com.studioos.server.advertisement.AdvertisementRepository;
import com.studioos.server.advertisement.campaign.dto.AdCampaignReportResponse;
import com.studioos.server.shared.exceptions.StudioosException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdCampaignReportService {

    private final AdCampaignRepository adCampaignRepository;
    private final AdBudgetRepository adBudgetRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdImpressionRepository adImpressionRepository;
    private final AdClickRepository adClickRepository;

    @Transactional(readOnly = true)
    public AdCampaignReportResponse getReport(Integer advertiserId, String campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> StudioosException.notFound("Campaign not found"));

        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw StudioosException.forbidden("You do not own this campaign");
        }

        AdBudget budget = adBudgetRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> StudioosException.notFound("Campaign budget not found"));

        long impressions = adImpressionRepository.countByCampaignId(campaignId);
        long clicks = adClickRepository.countByCampaignId(campaignId);
        long advertisements = advertisementRepository.countByCampaignId(campaignId);

        Double ctr = impressions == 0 ? null : (double) clicks / impressions;

        return AdCampaignReportResponse.builder()
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .status(campaign.getStatus())
                .paymentStatus(campaign.getPaymentStatus())
                .totalBudget(budget.getTotalBudget())
                .spentBudget(budget.getSpentBudget())
                .remainingBudget(budget.getRemainingBudget())
                .advertisementCount(advertisements)
                .impressions(impressions)
                .clicks(clicks)
                .clickThroughRate(ctr)
                .build();
    }
}

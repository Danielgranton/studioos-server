package com.studioos.server.advertisement.pricing;

import org.springframework.stereotype.Service;
import com.studioos.server.shared.enums.AdCreativeType;
import com.studioos.server.shared.enums.AdPlacement;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdvertisementPricingService {

    private final AdvertisementPricingEngine pricingEngine;
    private final AdvertisementPricingRepository advertisementPricingRepository;

    public AdvertisementPriceResult priceAndPersist(
            String campaignId, String advertisementId,
            AdCreativeType mediaType, AdPlacement placement,
            int durationDays, int totalBudget) {

        AdvertisementPriceResult result = pricingEngine.calculate(
                mediaType, placement, durationDays, totalBudget, null, null);

        AdvertisementPricing record = AdvertisementPricing.builder()
                .campaignId(campaignId)
                .advertisementId(advertisementId)
                .baseCpm(result.getBaseCpm())
                .placementMultiplier(result.getPlacementMultiplier())
                .placementRuleFound(result.isPlacementRuleFound())
                .targetingMultiplier(result.getTargetingMultiplier())
                .targetingRuleFound(result.isTargetingRuleFound())
                .durationMultiplier(result.getDurationMultiplier())
                .durationRuleFound(result.isDurationRuleFound())
                .budgetMultiplier(result.getBudgetMultiplier())
                .budgetRuleFound(result.isBudgetRuleFound())
                .promotionMultiplier(result.getPromotionMultiplier())
                .promotionRuleFound(result.isPromotionRuleFound())
                .finalCpm(result.getFinalCpm())
                .estimatedImpressions(result.getEstimatedImpressions())
                .currency(result.getCurrency())
                .build();

        advertisementPricingRepository.save(record);
        return result;
    }
}
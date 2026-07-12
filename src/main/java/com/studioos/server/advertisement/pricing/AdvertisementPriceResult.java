package com.studioos.server.advertisement.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AdvertisementPriceResult {
    private Integer baseCpm;

    private Double placementMultiplier;
    private boolean placementRuleFound;

    private Double targetingMultiplier;
    private boolean targetingRuleFound;

    private Double durationMultiplier;
    private boolean durationRuleFound;

    private Double budgetMultiplier;
    private boolean budgetRuleFound;

    private Double promotionMultiplier;
    private boolean promotionRuleFound;

    private Double finalCpm;
    private Long estimatedImpressions;
    private String currency;
}
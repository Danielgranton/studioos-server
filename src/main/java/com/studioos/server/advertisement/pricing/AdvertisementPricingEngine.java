package com.studioos.server.advertisement.pricing;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.studioos.server.shared.enums.AdCreativeType;
import com.studioos.server.shared.enums.AdPlacement;
import com.studioos.server.shared.enums.PricingRuleType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdvertisementPricingEngine {

    private final BaseCpmRateRepository baseCpmRateRepository;
    private final PricingRuleRepository pricingRuleRepository;

    public AdvertisementPriceResult calculate(
            AdCreativeType mediaType,
            AdPlacement placement,
            int durationDays,
            int totalBudget,
            String targetingTierKey,   // nullable — null means "broad", always 1.0 in v1
            String promotionCode       // nullable
    ) {
        int baseCpm = resolveBaseCpm(mediaType);

        RuleLookup placementLookup = lookupExact(PricingRuleType.PLACEMENT, placement.name());
        RuleLookup targetingLookup = targetingTierKey == null
                ? RuleLookup.defaultedNoKey()
                : lookupExact(PricingRuleType.TARGETING, targetingTierKey);
        RuleLookup durationLookup = lookupRange(PricingRuleType.DURATION_TIER, durationDays);
        RuleLookup budgetLookup = lookupRange(PricingRuleType.BUDGET_TIER, totalBudget);
        RuleLookup promotionLookup = promotionCode == null
                ? RuleLookup.defaultedNoKey()
                : lookupExact(PricingRuleType.PROMOTION, promotionCode);

        double finalCpm = baseCpm
                * placementLookup.multiplier()
                * targetingLookup.multiplier()
                * durationLookup.multiplier()
                * budgetLookup.multiplier()
                * promotionLookup.multiplier();

        long estimatedImpressions = Math.round((totalBudget / finalCpm) * 1000);

        return AdvertisementPriceResult.builder()
                .baseCpm(baseCpm)
                .placementMultiplier(placementLookup.multiplier())
                .placementRuleFound(placementLookup.found())
                .targetingMultiplier(targetingLookup.multiplier())
                .targetingRuleFound(targetingLookup.found())
                .durationMultiplier(durationLookup.multiplier())
                .durationRuleFound(durationLookup.found())
                .budgetMultiplier(budgetLookup.multiplier())
                .budgetRuleFound(budgetLookup.found())
                .promotionMultiplier(promotionLookup.multiplier())
                .promotionRuleFound(promotionLookup.found())
                .finalCpm(finalCpm)
                .estimatedImpressions(estimatedImpressions)
                .currency("KES")
                .build();
    }

    private int resolveBaseCpm(AdCreativeType mediaType) {
        return baseCpmRateRepository.findByMediaTypeAndActiveTrue(mediaType)
                .map(BaseCpmRate::getBaseCpm)
                .orElseThrow(() -> new PricingConfigurationException(
                        "Base CPM for " + mediaType + " is not configured. Cannot price this advertisement."));
    }

    private RuleLookup lookupExact(PricingRuleType type, String key) {
        Optional<PricingRule> rule = pricingRuleRepository.findByTypeAndRuleKeyAndActiveTrue(type, key);
        if (rule.isEmpty()) {
            log.warn("No active pricing rule for {} = '{}'. Using default multiplier 1.0.", type, key);
            return RuleLookup.defaulted();
        }
        return new RuleLookup(rule.get().getMultiplier(), true);
    }

    private RuleLookup lookupRange(PricingRuleType type, int value) {
        List<PricingRule> rules = pricingRuleRepository.findByTypeAndActiveTrue(type);

        Optional<PricingRule> match = rules.stream()
                .filter(r -> r.getMinValue() != null && value >= r.getMinValue())
                .filter(r -> r.getMaxValue() == null || value <= r.getMaxValue())
                .findFirst();

        if (match.isEmpty()) {
            log.warn("No active pricing rule for {} covering value {}. Using default multiplier 1.0.", type, value);
            return RuleLookup.defaulted();
        }
        return new RuleLookup(match.get().getMultiplier(), true);
    }

    private record RuleLookup(double multiplier, boolean found) {
        static RuleLookup defaulted() {
            return new RuleLookup(1.0, false);
        }
        static RuleLookup defaultedNoKey() {
            // Distinct from "rule missing" — this means no targeting/promotion was
            // requested at all, not that a lookup failed. Still reports found=false
            // since there's genuinely nothing to report as "found."
            return new RuleLookup(1.0, false);
        }
    }
}
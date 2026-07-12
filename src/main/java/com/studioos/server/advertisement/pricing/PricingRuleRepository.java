package com.studioos.server.advertisement.pricing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.studioos.server.shared.enums.PricingRuleType;

public interface PricingRuleRepository extends JpaRepository<PricingRule, String> {

    Optional<PricingRule> findByTypeAndRuleKeyAndActiveTrue(PricingRuleType type, String ruleKey);

    // For range-match lookups (DURATION_TIER, BUDGET_TIER) — fetch all active rules of
    // that type and let the engine pick the matching range, since JPQL range-containment
    // queries with nullable maxValue (unbounded upper tier) are awkward to express safely.
    List<PricingRule> findByTypeAndActiveTrue(PricingRuleType type);
}
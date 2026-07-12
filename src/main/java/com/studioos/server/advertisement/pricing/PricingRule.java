package com.studioos.server.advertisement.pricing;

import com.studioos.server.shared.enums.PricingRuleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pricing_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PricingRuleType type;

    
    private String ruleKey;

    private Integer minValue;
    private Integer maxValue;

    @Column(nullable = false)
    private Double multiplier;

    @Builder.Default
    private Boolean active = true;

    private String description;
}
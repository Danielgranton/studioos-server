package com.studioos.server.advertisement.pricing;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "advertisement_pricing")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String campaignId;

    @Column(nullable = false)
    private String advertisementId;

    @Column(nullable = false)
    private Integer baseCpm;

    @Column(nullable = false)
    private Double placementMultiplier;
    @Column(nullable = false)
    private Boolean placementRuleFound;

    @Column(nullable = false)
    private Double targetingMultiplier;
    @Column(nullable = false)
    private Boolean targetingRuleFound;

    @Column(nullable = false)
    private Double durationMultiplier;
    @Column(nullable = false)
    private Boolean durationRuleFound;

    @Column(nullable = false)
    private Double budgetMultiplier;
    @Column(nullable = false)
    private Boolean budgetRuleFound;

    @Column(nullable = false)
    private Double promotionMultiplier;
    @Column(nullable = false)
    private Boolean promotionRuleFound;

    @Column(nullable = false)
    private Double finalCpm;

    @Column(nullable = false)
    private Long estimatedImpressions;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "KES";

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime calculatedAt;
}
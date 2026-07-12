package com.studioos.server.advertisement.campaign;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ad_budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String campaignId;

    @Column(nullable = false)
    private Integer totalBudget;

    private Integer dailyBudget;   // nullable — no daily cap if unset

    @Column(nullable = false)
    private Integer remainingBudget;

    @Column(nullable = false)
    @Builder.Default
    private Integer spentBudget = 0;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "KES";
}
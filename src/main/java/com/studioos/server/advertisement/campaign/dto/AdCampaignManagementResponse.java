package com.studioos.server.advertisement.campaign.dto;

import java.time.LocalDateTime;

import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;
import com.studioos.server.shared.enums.AdPlacement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AdCampaignManagementResponse {
    private String campaignId;
    private String title;
    private String description;
    private AdPlacement placement;
    private AdCampaignStatus status;
    private AdPaymentStatus paymentStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalBudget;
    private Integer spentBudget;
    private Integer remainingBudget;
    private LocalDateTime createdAt;
}

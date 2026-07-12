package com.studioos.server.advertisement.campaign.dto;

import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AdCampaignReportResponse {
    private String campaignId;
    private String title;
    private AdCampaignStatus status;
    private AdPaymentStatus paymentStatus;
    private Integer totalBudget;
    private Integer spentBudget;
    private Integer remainingBudget;
    private long advertisementCount;
    private long impressions;
    private long clicks;
    private Double clickThroughRate;
}

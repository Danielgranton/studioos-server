package com.studioos.server.advertisement.campaign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CampaignResponse {
    private String campaignId;
    private String status;
}
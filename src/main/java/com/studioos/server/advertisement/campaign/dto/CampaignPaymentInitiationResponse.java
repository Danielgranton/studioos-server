package com.studioos.server.advertisement.campaign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CampaignPaymentInitiationResponse {
    private String transactionId;
    private String status;
}
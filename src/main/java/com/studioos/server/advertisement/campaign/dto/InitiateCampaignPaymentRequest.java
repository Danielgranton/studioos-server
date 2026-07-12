package com.studioos.server.advertisement.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitiateCampaignPaymentRequest {
    @NotBlank
    private String phoneNumber;
}
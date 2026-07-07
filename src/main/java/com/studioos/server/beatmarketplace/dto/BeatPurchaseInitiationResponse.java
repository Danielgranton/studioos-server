package com.studioos.server.beatmarketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatPurchaseInitiationResponse {
    private String purchaseId;
    private String transactionId;
    private String status;
}
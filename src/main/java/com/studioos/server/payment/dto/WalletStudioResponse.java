package com.studioos.server.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletStudioResponse {
    private String studioId;
    private String studioName;
    private Integer availableBalance;
    private Integer pendingBalance;
    private Integer reservedBalance;
    private Integer withdrawnBalance;
}

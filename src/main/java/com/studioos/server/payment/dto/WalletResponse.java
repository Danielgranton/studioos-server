package com.studioos.server.payment.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Integer availableBalance;
    private Integer pendingBalance;
    private Integer reservedBalance;
    private Integer withdrawnBalance;
    private List<WalletStudioResponse> studios;
    private List<WalletTransactionResponse> transactions;
    private List<WalletWithdrawalResponse> withdrawals;
}

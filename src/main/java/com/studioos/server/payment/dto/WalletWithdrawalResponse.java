package com.studioos.server.payment.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletWithdrawalResponse {
    private String id;
    private String studioId;
    private Integer amount;
    private String status;
    private String mpesaPhoneNumber;
    private String mpesaReceiptNumber;
    private String rejectionReason;
    private LocalDateTime createdAt;
}

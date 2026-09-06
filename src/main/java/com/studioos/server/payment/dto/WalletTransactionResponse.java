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
public class WalletTransactionResponse {
    private String id;
    private String studioId;
    private String type;
    private String status;
    private Integer amount;
    private String description;
    private String mpesaReceiptNumber;
    private LocalDateTime createdAt;
}

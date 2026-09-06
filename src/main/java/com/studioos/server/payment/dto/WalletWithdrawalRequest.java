package com.studioos.server.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WalletWithdrawalRequest {
    @Min(value = 1, message = "Amount must be greater than zero")
    private int amount;

    @NotBlank(message = "M-Pesa phone number is required")
    private String phoneNumber;
}

package com.studioos.server.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PaymentInitiationResponse {
    private String transactionId;
    private String status;
}
package com.studioos.server.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parsed result from either an STK Push callback (C2B) or a B2C result callback.
 * Same shape works for both since Daraja's callback payloads are structurally similar.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MpesaCallbackResult {
    private boolean success;
    private String mpesaReceiptNumber;
    private int amount;
    private String resultDescription;
    private String referenceId; // our transactionId or withdrawalId, echoed back via AccountReference
}
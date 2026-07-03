package com.studioos.server.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an STK Push initiation call to Daraja — the immediate response,
 * not the final payment outcome (that comes later via callback).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StkPushInitiationResult {
    private boolean accepted;          // whether Safaricom accepted the request
    private String merchantRequestId;
    private String checkoutRequestId;  // needed to match the later callback
    private String responseDescription;
}
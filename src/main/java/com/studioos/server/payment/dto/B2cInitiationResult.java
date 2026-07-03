package com.studioos.server.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a B2C initiation call to Daraja — immediate response,
 * final outcome comes via handleMpesaB2cCallback.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class B2cInitiationResult {
    private boolean accepted;
    private String conversationId;
    private String originatorConversationId;
    private String responseDescription;
}
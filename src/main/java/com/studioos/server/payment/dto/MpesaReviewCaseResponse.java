package com.studioos.server.payment.dto;

import java.time.LocalDateTime;

import com.studioos.server.payment.Transaction;

public record MpesaReviewCaseResponse(
        String transactionId,
        String checkoutRequestId,
        int amount,
        String status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime lastCheckedAt,
        LocalDateTime flaggedAt) {

    public static MpesaReviewCaseResponse from(Transaction transaction) {
        return new MpesaReviewCaseResponse(
                transaction.getId(),
                transaction.getMpesaCheckoutRequestId(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getMpesaReviewReason(),
                transaction.getCreatedAt(),
                transaction.getMpesaStatusCheckedAt(),
                transaction.getMpesaReviewFlaggedAt());
    }
}

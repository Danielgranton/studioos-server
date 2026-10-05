package com.studioos.server.payment;

import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MpesaPaymentReconciliationScheduler {
    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final MpesaProperties properties;

    @Scheduled(fixedDelayString = "${mpesa.reconciliation-interval-ms:60000}")
    public void reconcileStaleBeatPayments() {
        LocalDateTime now = LocalDateTime.now();
        var candidates = transactionRepository.findReconciliationCandidates(
                TransactionType.BEAT_PURCHASE,
                TransactionStatus.PENDING,
                now.minusNanos(properties.getReconciliationGracePeriodMs() * 1_000_000),
                now.minusNanos(properties.getReconciliationRetryIntervalMs() * 1_000_000),
                PageRequest.of(0, Math.max(1, properties.getReconciliationBatchSize())));
        for (String transactionId : candidates) {
            try {
                paymentService.reconcileStaleTransaction(transactionId);
            } catch (Exception e) {
                log.warn("Could not reconcile pending beat payment {}: {}", transactionId, e.getMessage());
            }
        }
    }
}

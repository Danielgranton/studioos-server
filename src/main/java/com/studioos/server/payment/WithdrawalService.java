package com.studioos.server.payment;

import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.enums.WithdrawalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final WalletService walletService;
    private final MpesaService mpesaService; // filled in last

    /**
     * Step 1: Producer requests a withdrawal. Validates available balance,
     * creates a PENDING withdrawal record. Does NOT move money yet —
     * that only happens on approval.
     */
    @Transactional
    public Withdrawal requestWithdrawal(String studioId, int amount, String phoneNumber) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        Wallet wallet = walletService.getOrCreateStudioWallet(studioId);
        if (wallet.getAvailableBalance() < amount) {
            throw new IllegalStateException(
                    "Insufficient available balance: has " + wallet.getAvailableBalance() + ", requested " + amount);
        }

        Withdrawal withdrawal = withdrawalRepository.save(
                Withdrawal.builder()
                        .studioId(studioId)
                        .amount(amount)
                        .status(WithdrawalStatus.PENDING)
                        .mpesaPhoneNumber(phoneNumber)
                        .build()
        );

        writeAudit(AuditEventType.TRANSACTION_CREATED, withdrawal.getId(), "Withdrawal", null,
                "Withdrawal requested: studio=" + studioId + ", amount=" + amount);

        return withdrawal;
    }

    /**
     * Step 2: Admin (or auto-approval logic) approves the request. Re-validates
     * balance (it may have changed since the request was made), then triggers
     * the M-Pesa B2C payout. Does NOT debit the wallet yet — that happens on
     * confirmed payout success, not on approval.
     */
    @Transactional
    public Withdrawal approveWithdrawal(String withdrawalId) {
        Withdrawal withdrawal = getPendingWithdrawalOrThrow(withdrawalId);

        Wallet wallet = walletService.getOrCreateStudioWallet(withdrawal.getStudioId());
        if (wallet.getAvailableBalance() < withdrawal.getAmount()) {
            throw new IllegalStateException(
                    "Insufficient available balance at approval time: has "
                            + wallet.getAvailableBalance() + ", requested " + withdrawal.getAmount());
        }

        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        withdrawalRepository.save(withdrawal);

        mpesaService.initiateB2cPayout(withdrawal.getMpesaPhoneNumber(), withdrawal.getAmount(), withdrawal.getId());

        return withdrawal;
    }

    /**
     * Producer or admin rejects a pending request. No money movement involved
     * since nothing was ever debited yet.
     */
    @Transactional
    public Withdrawal rejectWithdrawal(String withdrawalId, String reason) {
        Withdrawal withdrawal = getPendingWithdrawalOrThrow(withdrawalId);

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setRejectionReason(reason);
        withdrawalRepository.save(withdrawal);

        writeAudit(AuditEventType.TRANSACTION_CREATED, withdrawal.getId(), "Withdrawal", null,
                "Withdrawal rejected: " + reason);

        return withdrawal;
    }

    /**
     * Step 3: M-Pesa B2C callback confirms the payout result. On success,
     * debits the wallet (available -> withdrawn) and creates the WITHDRAWAL
     * transaction. On failure, marks the withdrawal FAILED-equivalent —
     * note WithdrawalStatus has no FAILED state, so we fall back to REJECTED
     * with a reason. Flagging this below.
     */
    @Transactional
    public Withdrawal handleMpesaB2cCallback(String withdrawalId, boolean success, String mpesaReceiptNumber) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.APPROVED) {
            throw new IllegalStateException(
                    "Withdrawal " + withdrawalId + " is not APPROVED (current: " + withdrawal.getStatus() + ")");
        }

        if (!success) {
            withdrawal.setStatus(WithdrawalStatus.REJECTED);
            withdrawal.setRejectionReason("M-Pesa B2C transfer failed");
            return withdrawalRepository.save(withdrawal);
        }

        Wallet wallet = walletService.getOrCreateStudioWallet(withdrawal.getStudioId());
        walletService.debitForWithdrawal(wallet.getId(), withdrawal.getAmount());

        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                        .type(TransactionType.WITHDRAWAL)
                        .status(TransactionStatus.SUCCESS)
                        .amount(withdrawal.getAmount())
                        .studioId(withdrawal.getStudioId())
                        .mpesaReceiptNumber(mpesaReceiptNumber)
                        .mpesaPhoneNumber(withdrawal.getMpesaPhoneNumber())
                        .description("Withdrawal payout for " + withdrawal.getId())
                        .build()
        );

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setTransactionId(transaction.getId());
        withdrawal.setMpesaReceiptNumber(mpesaReceiptNumber);
        withdrawalRepository.save(withdrawal);

        writeAudit(AuditEventType.WALLET_UPDATED, wallet.getId(), "Wallet", null,
                "Wallet debited " + withdrawal.getAmount() + " for withdrawal " + withdrawal.getId());

        return withdrawal;
    }

    private Withdrawal getPendingWithdrawalOrThrow(String withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException(
                    "Withdrawal " + withdrawalId + " is not PENDING (current: " + withdrawal.getStatus() + ")");
        }
        return withdrawal;
    }

    private void writeAudit(AuditEventType type, String entityId, String entityType, Integer userId, String description) {
        auditLogRepository.save(
                AuditLog.builder()
                        .eventType(type)
                        .entityId(entityId)
                        .entityType(entityType)
                        .userId(userId)
                        .description(description)
                        .build()
        );
    }
}
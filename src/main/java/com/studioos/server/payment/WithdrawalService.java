package com.studioos.server.payment;

import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.payment.dto.B2cInitiationResult;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.enums.WithdrawalStatus;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final WalletService walletService;
    private final StudioRepository studioRepository;
    private final NotificationServiceImpl notificationService;
    private final MpesaService mpesaService; // filled in last

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

        walletService.reserveForWithdrawal(wallet.getId(), withdrawal.getAmount());

        B2cInitiationResult result;
        try {
            result = mpesaService.initiateB2cPayout(
                    withdrawal.getMpesaPhoneNumber(), withdrawal.getAmount(), withdrawal.getId());
        } catch (RuntimeException e) {
            walletService.releaseReservedWithdrawal(wallet.getId(), withdrawal.getAmount());
            withdrawal.setStatus(WithdrawalStatus.PENDING);
            withdrawalRepository.save(withdrawal);
            throw e;
        }

        if (!result.isAccepted()) {
            walletService.releaseReservedWithdrawal(wallet.getId(), withdrawal.getAmount());
            withdrawal.setStatus(WithdrawalStatus.PENDING);
            withdrawalRepository.save(withdrawal);
            throw new IllegalStateException("B2C payout was not accepted: " + result.getResponseDescription());
        }

        return withdrawal;
    }

    @Transactional
    public Withdrawal rejectWithdrawal(String withdrawalId, String reason) {
        Withdrawal withdrawal = getPendingWithdrawalOrThrow(withdrawalId);

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setRejectionReason(reason);
        withdrawalRepository.save(withdrawal);

        writeAudit(AuditEventType.TRANSACTION_CREATED, withdrawal.getId(), "Withdrawal", null,
                "Withdrawal rejected: " + reason);

        notifyWithdrawalRejected(withdrawal, reason);

        return withdrawal;
    }

    @Transactional
    public Withdrawal handleMpesaB2cCallback(String withdrawalId, boolean success, String mpesaReceiptNumber) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found: " + withdrawalId));

        if (withdrawal.getStatus() == WithdrawalStatus.COMPLETED) {
            return withdrawal;
        }

        if (withdrawal.getStatus() == WithdrawalStatus.REJECTED) {
            return withdrawal;
        }

        if (withdrawal.getStatus() != WithdrawalStatus.APPROVED) {
            throw new IllegalStateException(
                    "Withdrawal " + withdrawalId + " is not APPROVED (current: " + withdrawal.getStatus() + ")");
        }

        if (!success) {
            Wallet wallet = walletService.getOrCreateStudioWallet(withdrawal.getStudioId());
            walletService.releaseReservedWithdrawal(wallet.getId(), withdrawal.getAmount());
            withdrawal.setStatus(WithdrawalStatus.REJECTED);
            withdrawal.setRejectionReason("M-Pesa B2C transfer failed");
            withdrawalRepository.save(withdrawal);
            notifyWithdrawalFailed(withdrawal);
            return withdrawal;
        }

        Wallet wallet = walletService.getOrCreateStudioWallet(withdrawal.getStudioId());
        walletService.commitReservedWithdrawal(wallet.getId(), withdrawal.getAmount());

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

        notifyWithdrawalCompleted(withdrawal, mpesaReceiptNumber);

        return withdrawal;
    }

    // ─── Notifications ───

    private void notifyWithdrawalCompleted(Withdrawal withdrawal, String mpesaReceiptNumber) {
        try {
            Integer ownerId = resolveOwnerId(withdrawal.getStudioId());
            if (ownerId == null) return;
            notificationService.createNotification(buildRequest(
                    ownerId, NotificationType.WALLET_TRANSACTION,
                    "Payout sent",
                    "KES " + withdrawal.getAmount() + " has been sent to your M-Pesa (receipt: "
                            + mpesaReceiptNumber + ").",
                    withdrawal.getId()
            ));
        } catch (Exception e) {
            log.error("Failed to send withdrawal-completed notification for {}: {}", withdrawal.getId(), e.getMessage());
        }
    }

    private void notifyWithdrawalFailed(Withdrawal withdrawal) {
        try {
            Integer ownerId = resolveOwnerId(withdrawal.getStudioId());
            if (ownerId == null) return;
            notificationService.createNotification(buildRequest(
                    ownerId, NotificationType.WALLET_TRANSACTION,
                    "Payout failed",
                    "Your withdrawal of KES " + withdrawal.getAmount()
                            + " could not be completed. Your funds remain in your wallet — please try again.",
                    withdrawal.getId()
            ));
        } catch (Exception e) {
            log.error("Failed to send withdrawal-failed notification for {}: {}", withdrawal.getId(), e.getMessage());
        }
    }

    private void notifyWithdrawalRejected(Withdrawal withdrawal, String reason) {
        try {
            Integer ownerId = resolveOwnerId(withdrawal.getStudioId());
            if (ownerId == null) return;
            notificationService.createNotification(buildRequest(
                    ownerId, NotificationType.WALLET_TRANSACTION,
                    "Withdrawal rejected",
                    "Your withdrawal request of KES " + withdrawal.getAmount() + " was rejected: " + reason,
                    withdrawal.getId()
            ));
        } catch (Exception e) {
            log.error("Failed to send withdrawal-rejected notification for {}: {}", withdrawal.getId(), e.getMessage());
        }
    }

    private Integer resolveOwnerId(String studioId) {
        return studioRepository.findById(studioId)
                .map(Studio::getOwnerId)
                .orElse(null);
    }

    private CreateNotificationRequest buildRequest(Integer userId, NotificationType type, String title,
                                                    String message, String relatedEntityId) {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setTitle(title);
        request.setMessage(message);
        request.setRelatedEntityId(relatedEntityId);
        return request;
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

package com.studioos.server.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.booking.Booking;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.enums.CommissionContext;
import com.studioos.server.shared.enums.EscrowStatus;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final WalletService walletService;
    private final CommissionService commissionService;

    /**
     * Creates escrow on successful booking payment. Full amount moves into the
     * studio wallet's pending balance until release or refund.
     */
    @Transactional
    public Escrow holdEscrow(Booking booking, Transaction paymentTransaction) {
        Wallet studioWallet = walletService.getOrCreateStudioWallet(booking.getStudioId());
        walletService.addToPending(studioWallet.getId(), paymentTransaction.getAmount());

        Escrow escrow = escrowRepository.save(
                Escrow.builder()
                        .bookingId(booking.getId())
                        .transactionId(paymentTransaction.getId())
                        .status(EscrowStatus.HELD)
                        .amount(paymentTransaction.getAmount())
                        .build()
        );

        writeAudit(AuditEventType.ESCROW_CREATED, escrow.getId(), "Escrow", booking.getArtistId(),
                "Escrow held for booking " + booking.getId());

        return escrow;
    }

    /**
     * Releases escrow on booking DELIVERED. Producer gets amount minus commission,
     * platform wallet gets the commission.
     */
    @Transactional
    public Escrow releaseEscrow(String bookingId) {
        Escrow escrow = getHeldEscrowOrThrow(bookingId);

        int commission = commissionService.calculateCommission(escrow.getAmount(), CommissionContext.BOOKING);
        int netAmount = escrow.getAmount() - commission;

        Wallet studioWallet = walletService.getOrCreateStudioWallet(getStudioIdFromEscrow(escrow));
        Wallet platformWallet = walletService.getOrCreatePlatformWallet();

        walletService.removeFromPending(studioWallet.getId(), escrow.getAmount());
        walletService.creditAvailable(studioWallet.getId(), netAmount);
        walletService.creditAvailable(platformWallet.getId(), commission);

        Transaction commissionTx = transactionRepository.save(
                Transaction.builder()
                        .type(TransactionType.COMMISSION)
                        .status(TransactionStatus.SUCCESS)
                        .amount(commission)
                        .bookingId(bookingId)
                        .studioId(studioWallet.getStudioId())
                        .description("Commission on booking " + bookingId)
                        .build()
        );

        escrow.setStatus(EscrowStatus.RELEASED);
        escrowRepository.save(escrow);

        writeAudit(AuditEventType.ESCROW_RELEASED, escrow.getId(), "Escrow", null,
                "Escrow released for booking " + bookingId + ", commission=" + commission);
        writeAudit(AuditEventType.WALLET_UPDATED, studioWallet.getId(), "Wallet", null,
                "Studio wallet credited " + netAmount + " for booking " + bookingId);
        writeAudit(AuditEventType.TRANSACTION_CREATED, commissionTx.getId(), "Transaction", null,
                "Commission transaction created for booking " + bookingId);

        return escrow;
    }

    /**
     * Refunds escrow when a producer cancels before RECORDING starts.
     * Platform still keeps commission since it facilitated the booking.
     * Returns the refund amount (amount - commission) so the caller can
     * trigger the actual M-Pesa reversal for that amount.
     */
    @Transactional
    public int refundEscrow(String bookingId) {
        Escrow escrow = getHeldEscrowOrThrow(bookingId);

        int commission = commissionService.calculateCommission(escrow.getAmount(), CommissionContext.BOOKING);
        int refundAmount = escrow.getAmount() - commission;

        Wallet studioWallet = walletService.getOrCreateStudioWallet(getStudioIdFromEscrow(escrow));
        Wallet platformWallet = walletService.getOrCreatePlatformWallet();

        // Full amount leaves pending — producer earns nothing on a cancelled booking.
        walletService.removeFromPending(studioWallet.getId(), escrow.getAmount());
        walletService.creditAvailable(platformWallet.getId(), commission);

        transactionRepository.save(
                Transaction.builder()
                        .type(TransactionType.REFUND)
                        .status(TransactionStatus.SUCCESS)
                        .amount(refundAmount)
                        .bookingId(bookingId)
                        .description("Refund for cancelled booking " + bookingId)
                        .build()
        );

        transactionRepository.save(
                Transaction.builder()
                        .type(TransactionType.COMMISSION)
                        .status(TransactionStatus.SUCCESS)
                        .amount(commission)
                        .bookingId(bookingId)
                        .studioId(studioWallet.getStudioId())
                        .description("Commission retained on cancelled booking " + bookingId)
                        .build()
        );

        escrow.setStatus(EscrowStatus.REFUNDED);
        escrowRepository.save(escrow);

        writeAudit(AuditEventType.ESCROW_REFUNDED, escrow.getId(), "Escrow", null,
                "Escrow refunded for cancelled booking " + bookingId + ", commission retained=" + commission);

        return refundAmount;
    }

    private Escrow getHeldEscrowOrThrow(String bookingId) {
        Escrow escrow = escrowRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No escrow found for booking " + bookingId));
        if (escrow.getStatus() != EscrowStatus.HELD) {
            throw new IllegalStateException("Escrow for booking " + bookingId + " is not HELD (current: " + escrow.getStatus() + ")");
        }
        return escrow;
    }

    private String getStudioIdFromEscrow(Escrow escrow) {
        // Escrow -> Transaction -> studioId isn't stored directly on Escrow, so we pull it
        // from the linked booking's transaction. Simpler: re-fetch via the Booking relation.
        return escrow.getBooking().getStudioId();
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
package com.studioos.server.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.payment.dto.StkPushInitiationResult;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;
    private final EscrowService escrowService;
    private final MpesaService mpesaService;

    /**
     * Step 1: Client initiates payment for a booking. Creates a PENDING transaction
     * and triggers the M-Pesa STK Push. Stores the checkoutRequestId returned by
     * Safaricom so the later callback (Step 2) can be matched back to this transaction.
     * Does NOT touch escrow yet — that only happens once the callback confirms success.
     */
    @Transactional
    public Transaction initiateBookingPayment(String bookingId, String phoneNumber) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getPaymentStatus() == BookingPaymentStatus.PAID) {
            throw new IllegalStateException("Booking " + bookingId + " is already fully paid");
        }
        if (booking.getTotalPrice() == null) {
            throw new IllegalStateException("Booking " + bookingId + " has no total price set");
        }

        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                        .type(TransactionType.BOOKING_PAYMENT)
                        .status(TransactionStatus.PENDING)
                        .amount(booking.getTotalPrice())
                        .bookingId(booking.getId())
                        .studioId(booking.getStudioId())
                        .userId(booking.getArtistId())
                        .mpesaPhoneNumber(phoneNumber)
                        .description("Booking payment for " + booking.getId())
                        .build()
        );

        StkPushInitiationResult stkResult = mpesaService.initiateStkPush(
                phoneNumber, booking.getTotalPrice(), transaction.getId());

        if (!stkResult.isAccepted()) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new IllegalStateException("STK Push was not accepted: " + stkResult.getResponseDescription());
        }

        transaction.setMpesaCheckoutRequestId(stkResult.getCheckoutRequestId());
        transactionRepository.save(transaction);

        writeAudit(AuditEventType.TRANSACTION_CREATED, transaction.getId(), "Transaction",
                booking.getArtistId(), "Booking payment initiated for " + booking.getId());

        return transaction;
    }

    /**
     * Step 2: M-Pesa callback confirms the payment result. Looked up by checkoutRequestId
     * since that's the only identifier Safaricom's callback payload carries — not our
     * internal transaction ID. On success, marks the transaction SUCCESS, moves the
     * booking to PAID, and hands off to EscrowService to hold the funds.
     */
    @Transactional
    public Transaction handleMpesaCallback(String checkoutRequestId, boolean success, String mpesaReceiptNumber) {
        Transaction transaction = transactionRepository.findByMpesaCheckoutRequestId(checkoutRequestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No transaction found for checkoutRequestId: " + checkoutRequestId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Transaction " + transaction.getId() + " already resolved (status: " + transaction.getStatus() + ")");
        }

        if (!success) {
            transaction.setStatus(TransactionStatus.FAILED);
            return transactionRepository.save(transaction);
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setMpesaReceiptNumber(mpesaReceiptNumber);
        transactionRepository.save(transaction);

        Booking booking = bookingRepository.findById(transaction.getBookingId())
                .orElseThrow(() -> new IllegalStateException("Booking not found for transaction " + transaction.getId()));
        booking.setPaymentStatus(BookingPaymentStatus.PAID);
        bookingRepository.save(booking);

        escrowService.holdEscrow(booking, transaction);

        return transaction;
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
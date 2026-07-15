package com.studioos.server.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.booking.events.BookingPaidEvent;
import com.studioos.server.payment.dto.StkPushInitiationResult;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.events.TransactionResolvedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;
    private final EscrowService escrowService;
    private final MpesaService mpesaService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public Transaction initiateBookingPayment(Integer requesterId, String bookingId, String phoneNumber) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getArtistId().equals(requesterId)) {
            throw new SecurityException("You cannot pay for this booking");
        }

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new IllegalStateException("Booking " + bookingId + " must be approved before payment");
        }

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
            transactionRepository.save(transaction);
            eventPublisher.publishEvent(
                    new TransactionResolvedEvent(transaction.getId(), transaction.getType(), false));
            return transaction;
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setMpesaReceiptNumber(mpesaReceiptNumber);
        transactionRepository.save(transaction);

        if (transaction.getType() == TransactionType.BOOKING_PAYMENT) {
            Booking booking = bookingRepository.findById(transaction.getBookingId())
                    .orElseThrow(() -> new IllegalStateException("Booking not found for transaction " + transaction.getId()));
            booking.setPaymentStatus(BookingPaymentStatus.PAID);
            bookingRepository.save(booking);

            escrowService.holdEscrow(booking, transaction);
            eventPublisher.publishEvent(BookingPaidEvent.builder()
                    .bookingId(booking.getId())
                    .studioId(booking.getStudioId())
                    .artistId(booking.getArtistId())
                    .transactionId(transaction.getId())
                    .build());
        }

        eventPublisher.publishEvent(
                new TransactionResolvedEvent(transaction.getId(), transaction.getType(), true));

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

    @Transactional
    public Transaction initiateBeatPurchasePayment(Integer buyerId, String studioId, Integer amount,
                                                    String phoneNumber, String description) {

        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                    .type(TransactionType.BEAT_PURCHASE)
                    .status(TransactionStatus.PENDING)
                    .amount(amount)
                    .studioId(studioId)
                    .userId(buyerId)
                    .mpesaPhoneNumber(phoneNumber)
                    .description(description)
                    .build()
        );

        StkPushInitiationResult stkResult = mpesaService.initiateStkPush(phoneNumber, amount, transaction.getId());

        if (!stkResult.isAccepted()) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new IllegalStateException("STK Push was not accepted: " + stkResult.getResponseDescription());
        }

        transaction.setMpesaCheckoutRequestId(stkResult.getCheckoutRequestId());
        transactionRepository.save(transaction);

        writeAudit(AuditEventType.TRANSACTION_CREATED, transaction.getId(), "Transaction",
                buyerId, "Beat purchase payment initiated: " + description);

        return transaction;
    }

    @Transactional
    public Transaction initiateAdCampaignPayment(Integer advertiserId, String studioId, Integer amount,
                                                String phoneNumber, String description) {

        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                        .type(TransactionType.AD_CAMPAIGN)
                        .status(TransactionStatus.PENDING)
                        .amount(amount)
                        .studioId(studioId)
                        .userId(advertiserId)
                        .mpesaPhoneNumber(phoneNumber)
                        .description(description)
                        .build()
        );

        StkPushInitiationResult stkResult = mpesaService.initiateStkPush(phoneNumber, amount, transaction.getId());

        if (!stkResult.isAccepted()) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new IllegalStateException("STK Push was not accepted: " + stkResult.getResponseDescription());
        }

        transaction.setMpesaCheckoutRequestId(stkResult.getCheckoutRequestId());
        transactionRepository.save(transaction);

        writeAudit(AuditEventType.TRANSACTION_CREATED, transaction.getId(), "Transaction",
                advertiserId, "Ad campaign payment initiated: " + description);

        return transaction;
    }
}

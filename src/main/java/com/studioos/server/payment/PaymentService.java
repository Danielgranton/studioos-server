package com.studioos.server.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.booking.events.BookingPaidEvent;
import com.studioos.server.payment.dto.StkPushInitiationResult;
import com.studioos.server.payment.dto.StkPushQueryResult;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.events.TransactionResolvedEvent;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

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
    private final UserRepository userRepository;
    private final NotificationServiceImpl notificationService;
    private final MpesaProperties mpesaProperties;

    @Transactional
    public Transaction initiateBookingPayment(Integer requesterId, String bookingId, String phoneNumber) {
        String normalizedPhone = MpesaPhoneNumber.normalize(phoneNumber);
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
                        .mpesaPhoneNumber(normalizedPhone)
                        .description("Booking payment for " + booking.getId())
                        .build()
        );

        StkPushInitiationResult stkResult = mpesaService.initiateStkPush(
                normalizedPhone, booking.getTotalPrice(), transaction.getId());

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
    public Transaction handleMpesaCallback(
            String checkoutRequestId, boolean success, int callbackAmount, String mpesaReceiptNumber) {
        Transaction transaction = transactionRepository.findByCheckoutRequestIdForUpdate(checkoutRequestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No transaction found for checkoutRequestId: " + checkoutRequestId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            if ((success && transaction.getStatus() == TransactionStatus.SUCCESS
                    && (transaction.getMpesaReceiptNumber() == null
                            || java.util.Objects.equals(transaction.getMpesaReceiptNumber(), mpesaReceiptNumber)))
                    || (!success && transaction.getStatus() == TransactionStatus.FAILED)) {
                return transaction;
            }
            throw new IllegalStateException(
                    "Transaction " + transaction.getId() + " already resolved (status: " + transaction.getStatus() + ")");
        }

        if (success && (callbackAmount != transaction.getAmount()
                || mpesaReceiptNumber == null || mpesaReceiptNumber.isBlank())) {
            throw new IllegalArgumentException("M-Pesa callback amount or receipt does not match the transaction");
        }

        return reconcileLockedTransaction(transaction, success ? mpesaReceiptNumber : null);
    }

    @Transactional
    public void reconcileStaleTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId).orElse(null);
        if (transaction == null || transaction.getStatus() != TransactionStatus.PENDING
                || transaction.getMpesaCheckoutRequestId() == null) return;
        reconcileLockedTransaction(transaction, null);
    }

    private Transaction reconcileLockedTransaction(Transaction transaction, String callbackReceipt) {
        if (transaction == null || transaction.getStatus() != TransactionStatus.PENDING) return transaction;
        StkPushQueryResult query = mpesaService.queryStkPush(transaction.getMpesaCheckoutRequestId());
        transaction.setMpesaStatusCheckedAt(LocalDateTime.now());

        if (query.status() == StkPushQueryResult.Status.PENDING
                || query.status() == StkPushQueryResult.Status.UNKNOWN) {
            if (transaction.getType() == TransactionType.BEAT_PURCHASE
                    && transaction.getCreatedAt() != null
                    && transaction.getCreatedAt().isBefore(LocalDateTime.now().minusNanos(
                            mpesaProperties.getReviewAfterMs() * 1_000_000))) {
                flagForManualReview(transaction, query.description());
            }
            return transactionRepository.save(transaction);
        }
        if (query.status() == StkPushQueryResult.Status.FAILED) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            eventPublisher.publishEvent(
                    new TransactionResolvedEvent(transaction.getId(), transaction.getType(), false));
            return transaction;
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setMpesaReceiptNumber(callbackReceipt);
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

    private void flagForManualReview(Transaction transaction, String queryDescription) {
        if (transaction.isMpesaReviewRequired()) return;
        transaction.setMpesaReviewRequired(true);
        transaction.setMpesaReviewFlaggedAt(LocalDateTime.now());
        transaction.setMpesaReviewReason(queryDescription == null || queryDescription.isBlank()
                ? "Daraja has not returned a conclusive payment result."
                : queryDescription);

        for (Role role : new Role[] { Role.ADMIN, Role.SUPER_ADMIN }) {
            for (User admin : userRepository.findByRole(role)) {
                try {
                    CreateNotificationRequest request = new CreateNotificationRequest();
                    request.setUserId(admin.getId());
                    request.setType(NotificationType.PAYMENT_RECONCILIATION_REQUIRED);
                    request.setTitle("M-Pesa payment needs review");
                    request.setMessage("Beat purchase transaction " + transaction.getId()
                            + " has remained unresolved for over "
                            + Math.max(1, mpesaProperties.getReviewAfterMs() / 3_600_000)
                            + " hours. The exclusive beat remains reserved.");
                    request.setRelatedEntityId(transaction.getId());
                    notificationService.createNotification(request);
                } catch (Exception e) {
                    // Do not lose the persistent review flag if notification delivery is unavailable.
                    org.slf4j.LoggerFactory.getLogger(PaymentService.class)
                            .error("Could not notify admin {} about payment review {}: {}",
                                    admin.getId(), transaction.getId(), e.getMessage());
                }
            }
        }
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
        String normalizedPhone = MpesaPhoneNumber.normalize(phoneNumber);

        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                    .type(TransactionType.BEAT_PURCHASE)
                    .status(TransactionStatus.PENDING)
                    .amount(amount)
                    .studioId(studioId)
                    .userId(buyerId)
                    .mpesaPhoneNumber(normalizedPhone)
                    .description(description)
                    .build()
        );

        StkPushInitiationResult stkResult = mpesaService.initiateStkPush(normalizedPhone, amount, transaction.getId());

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
    public Transaction initiateServiceBookingPayment(Integer requesterId, String studioId, Integer amount,
            String phoneNumber, String serviceBookingId, String description) {
        String normalizedPhone = MpesaPhoneNumber.normalize(phoneNumber);
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .type(TransactionType.SERVICE_BOOKING_PAYMENT)
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .studioId(studioId)
                .userId(requesterId)
                .serviceBookingId(serviceBookingId)
                .mpesaPhoneNumber(normalizedPhone)
                .description(description)
                .build());
        StkPushInitiationResult stkResult = mpesaService.initiateStkPush(normalizedPhone, amount, transaction.getId());
        if (!stkResult.isAccepted()) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new IllegalStateException("STK Push was not accepted: " + stkResult.getResponseDescription());
        }
        transaction.setMpesaCheckoutRequestId(stkResult.getCheckoutRequestId());
        transactionRepository.save(transaction);
        writeAudit(AuditEventType.TRANSACTION_CREATED, transaction.getId(), "Transaction", requesterId,
                "Service booking payment initiated: " + serviceBookingId);
        return transaction;
    }

    @Transactional
    public Transaction initiateAdCampaignPayment(Integer advertiserId, String studioId, Integer amount,
                                                String phoneNumber, String description) {
        String normalizedPhone = MpesaPhoneNumber.normalize(phoneNumber);

        Transaction transaction = transactionRepository.save(
                Transaction.builder()
                        .type(TransactionType.AD_CAMPAIGN)
                        .status(TransactionStatus.PENDING)
                        .amount(amount)
                        .studioId(studioId)
                        .userId(advertiserId)
                        .mpesaPhoneNumber(normalizedPhone)
                        .description(description)
                        .build()
        );

        StkPushInitiationResult stkResult = mpesaService.initiateStkPush(normalizedPhone, amount, transaction.getId());

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

package com.studioos.server.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.TransactionStatus;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.payment.dto.StkPushQueryResult;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private EscrowService escrowService;
    @Mock
    private MpesaService mpesaService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationServiceImpl notificationService;
    @Mock
    private MpesaProperties mpesaProperties;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void initiateBookingPaymentRejectsNonOwner() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .artistId(10)
                .studioId("studio-1")
                .status(BookingStatus.PENDING)
                .paymentStatus(BookingPaymentStatus.BOOKED)
                .totalPrice(5000)
                .build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.initiateBookingPayment(99, "booking-1", "+254700000000"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void initiateBookingPaymentRejectsUnapprovedBooking() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .artistId(10)
                .studioId("studio-1")
                .status(BookingStatus.PENDING)
                .paymentStatus(BookingPaymentStatus.BOOKED)
                .totalPrice(5000)
                .build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.initiateBookingPayment(10, "booking-1", "+254700000000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be approved");
    }

    @Test
    void successfulMpesaCallbackRejectsAmountMismatch() {
        Transaction transaction = Transaction.builder()
                .id("transaction-1")
                .type(TransactionType.BEAT_PURCHASE)
                .status(TransactionStatus.PENDING)
                .amount(1500)
                .mpesaCheckoutRequestId("checkout-1")
                .build();
        when(transactionRepository.findByCheckoutRequestIdForUpdate("checkout-1"))
                .thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> paymentService.handleMpesaCallback(
                "checkout-1", true, 1200, "receipt-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount or receipt");

        verify(transactionRepository, never()).save(transaction);
    }

    @Test
    void callbackDoesNotResolvePaymentWhenDarajaCannotConfirmIt() {
        Transaction transaction = Transaction.builder()
                .id("transaction-2")
                .type(TransactionType.BEAT_PURCHASE)
                .status(TransactionStatus.PENDING)
                .amount(1500)
                .mpesaCheckoutRequestId("checkout-2")
                .build();
        when(transactionRepository.findByCheckoutRequestIdForUpdate("checkout-2"))
                .thenReturn(Optional.of(transaction));
        when(mpesaService.queryStkPush("checkout-2"))
                .thenReturn(StkPushQueryResult.unknown("temporarily unavailable"));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        paymentService.handleMpesaCallback("checkout-2", false, 0, null);

        org.assertj.core.api.Assertions.assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void callbackResolvesSuccessOnlyAfterDarajaConfirmsCheckout() {
        Transaction transaction = Transaction.builder()
                .id("transaction-3")
                .type(TransactionType.BEAT_PURCHASE)
                .status(TransactionStatus.PENDING)
                .amount(1500)
                .mpesaCheckoutRequestId("checkout-3")
                .build();
        when(transactionRepository.findByCheckoutRequestIdForUpdate("checkout-3"))
                .thenReturn(Optional.of(transaction));
        when(mpesaService.queryStkPush("checkout-3"))
                .thenReturn(StkPushQueryResult.success("Payment confirmed"));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        paymentService.handleMpesaCallback("checkout-3", true, 1500, "receipt-3");

        org.assertj.core.api.Assertions.assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(
                com.studioos.server.shared.events.TransactionResolvedEvent.class));
    }

    @Test
    void oldUnresolvedBeatPaymentIsFlaggedForAdminReview() {
        Transaction transaction = Transaction.builder()
                .id("transaction-old")
                .type(TransactionType.BEAT_PURCHASE)
                .status(TransactionStatus.PENDING)
                .amount(1500)
                .createdAt(LocalDateTime.now().minusDays(2))
                .mpesaCheckoutRequestId("checkout-old")
                .build();
        when(transactionRepository.findByCheckoutRequestIdForUpdate("checkout-old"))
                .thenReturn(Optional.of(transaction));
        when(mpesaService.queryStkPush("checkout-old"))
                .thenReturn(StkPushQueryResult.unknown("Daraja unavailable"));
        when(mpesaProperties.getReviewAfterMs()).thenReturn(86_400_000L);
        when(userRepository.findByRole(org.mockito.ArgumentMatchers.any(Role.class))).thenReturn(List.of());
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        paymentService.handleMpesaCallback("checkout-old", false, 0, null);

        org.assertj.core.api.Assertions.assertThat(transaction.isMpesaReviewRequired()).isTrue();
        org.assertj.core.api.Assertions.assertThat(transaction.getMpesaReviewReason()).isEqualTo("Daraja unavailable");
        org.assertj.core.api.Assertions.assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }
}

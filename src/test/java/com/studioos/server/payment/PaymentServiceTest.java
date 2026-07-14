package com.studioos.server.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;

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
}

package com.studioos.server.booking;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.payment.EscrowService;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private StudioRepository studioRepository;
    @Mock
    private EscrowService escrowService;
    @Mock
    private NotificationServiceImpl notificationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void cancelBookingRefundsEscrowWhenBookingWasPaid() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .studioId("studio-1")
                .artistId(10)
                .status(BookingStatus.APPROVED)
                .paymentStatus(BookingPaymentStatus.PAID)
                .sessionDate(LocalDateTime.now().plusDays(1))
                .durationHours(2)
                .totalPrice(5000)
                .build();
        Studio studio = Studio.builder().id("studio-1").ownerId(11).studioName("Room A").build();
        User artist = User.builder().id(10).email("artist@example.com").name("Artist").role(Role.ARTIST).build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(studioRepository.findById("studio-1")).thenReturn(Optional.of(studio));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.cancelBooking(artist, "booking-1");

        verify(escrowService).refundEscrow("booking-1");
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBookingRejectsUnauthorizedUser() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .studioId("studio-1")
                .artistId(10)
                .status(BookingStatus.PENDING)
                .paymentStatus(BookingPaymentStatus.BOOKED)
                .sessionDate(LocalDateTime.now().plusDays(1))
                .durationHours(2)
                .build();
        Studio studio = Studio.builder().id("studio-1").ownerId(11).studioName("Room A").build();
        User otherUser = User.builder().id(99).email("other@example.com").name("Other").role(Role.USER).build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(studioRepository.findById("studio-1")).thenReturn(Optional.of(studio));

        assertThatThrownBy(() -> bookingService.cancelBooking(otherUser, "booking-1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void cancelBookingRejectsInProgressBookings() {
        Booking booking = Booking.builder()
                .id("booking-1")
                .studioId("studio-1")
                .artistId(10)
                .status(BookingStatus.RECORDING)
                .paymentStatus(BookingPaymentStatus.PAID)
                .sessionDate(LocalDateTime.now().plusDays(1))
                .durationHours(2)
                .build();
        Studio studio = Studio.builder().id("studio-1").ownerId(11).studioName("Room A").build();
        User artist = User.builder().id(10).email("artist@example.com").name("Artist").role(Role.ARTIST).build();

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(studioRepository.findById("studio-1")).thenReturn(Optional.of(studio));

        assertThatThrownBy(() -> bookingService.cancelBooking(artist, "booking-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already in progress");
    }
}

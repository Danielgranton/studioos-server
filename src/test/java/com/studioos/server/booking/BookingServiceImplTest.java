package com.studioos.server.booking;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.studioos.server.payment.EscrowService;
import com.studioos.server.payment.PaymentService;
import com.studioos.server.booking.dto.CreateBookingRequest;
import com.studioos.server.booking.dto.UpdateBookingRequest;
import com.studioos.server.booking.dto.BookingResponse;
import com.studioos.server.booking.events.BookingCreatedEvent;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;
import org.springframework.context.ApplicationEventPublisher;

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
    private PaymentService paymentService;
    @Mock
    private EscrowService escrowService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void rejectsThirdAttemptForSameSessionAfterTwoExpiries() {
        LocalDateTime sessionDate = LocalDateTime.now().plusDays(2);
        User artist = User.builder().id(10).email("artist@example.com").name("Artist").role(Role.ARTIST).build();
        Studio studio = Studio.builder().id("studio-1").ownerId(11).studioName("Room A").build();
        CreateBookingRequest request = new CreateBookingRequest();
        request.setStudioId("studio-1");
        request.setSessionDate(sessionDate);
        request.setDurationHours(2);

        when(studioRepository.findById("studio-1")).thenReturn(Optional.of(studio));
        when(bookingRepository.sumExpiredAttempts(
                10, "studio-1", sessionDate, 2, BookingStatus.EXPIRED)).thenReturn(2L);

        assertThatThrownBy(() -> bookingService.createBooking(artist, request))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("expired twice");
        verify(bookingRepository).sumExpiredAttempts(
                10, "studio-1", sessionDate, 2, BookingStatus.EXPIRED);
    }

    @Test
    void expiredBookingCanBeEditedAndResubmittedOnce() {
        LocalDateTime nextSession = LocalDateTime.now().plusDays(3);
        Booking booking = Booking.builder().id("booking-1").studioId("studio-1").artistId(10)
                .sessionDate(LocalDateTime.now().minusDays(1)).durationHours(2).status(BookingStatus.EXPIRED)
                .paymentStatus(BookingPaymentStatus.BOOKED).totalPrice(5000).attemptCount(1).build();
        Studio studio = Studio.builder().id("studio-1").ownerId(11).studioName("Room A").build();
        User artist = User.builder().id(10).email("artist@example.com").name("Artist").role(Role.ARTIST).build();
        UpdateBookingRequest request = new UpdateBookingRequest();
        request.setSessionDate(nextSession);
        request.setDurationHours(3);
        request.setNotes("Updated session details");

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));
        when(studioRepository.findById("studio-1")).thenReturn(Optional.of(studio));
        when(bookingRepository.findConflictingBookings("studio-1", nextSession, nextSession.plusHours(3)))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.updateArtistBooking(artist, "booking-1", request);

        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        org.assertj.core.api.Assertions.assertThat(response.getAttemptCount()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(response.getTotalPrice()).isNull();
        verify(applicationEventPublisher).publishEvent(any(BookingCreatedEvent.class));
    }

    @Test
    void rejectsRetryAfterBothAttemptsHaveExpired() {
        Booking booking = Booking.builder().id("booking-1").studioId("studio-1").artistId(10)
                .sessionDate(LocalDateTime.now().minusDays(1)).durationHours(2).status(BookingStatus.EXPIRED)
                .paymentStatus(BookingPaymentStatus.BOOKED).attemptCount(2).build();
        User artist = User.builder().id(10).email("artist@example.com").name("Artist").role(Role.ARTIST).build();
        UpdateBookingRequest request = new UpdateBookingRequest();
        request.setSessionDate(LocalDateTime.now().plusDays(3));
        request.setDurationHours(2);

        when(bookingRepository.findById("booking-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.updateArtistBooking(artist, "booking-1", request))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("used both attempts");
    }

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

package com.studioos.server.session;

import com.studioos.server.booking.BookingRepository;
import com.studioos.server.booking.events.BookingCancelledEvent;
import com.studioos.server.booking.events.BookingExpiredEvent;
import com.studioos.server.booking.events.BookingPaidEvent;
import com.studioos.server.shared.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSessionListener {

    private final BookingRepository bookingRepository;
    private final RecordingSessionService recordingSessionService;

    @EventListener
    public void onBookingPaid(BookingPaidEvent event) {
        bookingRepository.findById(event.getBookingId()).ifPresentOrElse(booking -> {
            if (booking.getStatus() != BookingStatus.APPROVED) {
                log.warn("Skipping session creation for booking {} because it is not approved", booking.getId());
                return;
            }
            recordingSessionService.createSession(booking);
        }, () -> log.warn("Skipping session creation for missing booking {}", event.getBookingId()));
    }

    @EventListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        bookingRepository.findById(event.getBookingId()).ifPresent(booking ->
                recordingSessionService.cancelSessionByBooking(booking.getId(), "Booking cancelled"));
    }

    @EventListener
    public void onBookingExpired(BookingExpiredEvent event) {
        bookingRepository.findById(event.getBookingId()).ifPresent(booking ->
                recordingSessionService.cancelSessionByBooking(booking.getId(), "Booking expired"));
    }
}

package com.studioos.server.booking;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.booking.events.BookingExpiredEvent;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private static final int EXPIRY_MINUTES = 15;

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Scheduled(fixedDelayString = "${booking.expiry-scan-ms:300000}")
    @Transactional
    public void expireUnpaidBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRY_MINUTES);
        List<Booking> expired = bookingRepository.findByStatusAndPaymentStatusAndCreatedAtBefore(
                BookingStatus.PENDING,
                BookingPaymentStatus.BOOKED,
                cutoff);

        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(BookingExpiredEvent.builder()
                    .bookingId(booking.getId())
                    .studioId(booking.getStudioId())
                    .artistId(booking.getArtistId())
                    .build());
            log.info("Expired booking {}", booking.getId());
        }
    }
}

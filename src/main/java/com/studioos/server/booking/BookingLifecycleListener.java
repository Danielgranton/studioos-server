package com.studioos.server.booking;

import com.studioos.server.booking.events.BookingCancelledEvent;
import com.studioos.server.booking.events.BookingConfirmedEvent;
import com.studioos.server.booking.events.BookingCreatedEvent;
import com.studioos.server.booking.events.BookingExpiredEvent;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;
import com.studioos.server.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingLifecycleListener {

    private final NotificationServiceImpl notificationService;
    private final UserRepository userRepository;
    private final StudioRepository studioRepository;

    @EventListener
    public void onBookingCreated(BookingCreatedEvent event) {
        bookingOwner(event.getStudioId()).ifPresent(owner ->
                send(owner.getId(), NotificationType.BOOKING_REQUEST,
                        "New booking request",
                        "A new booking request is waiting for your review.",
                        event.getBookingId()));
    }

    @EventListener
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        send(event.getArtistId(), NotificationType.BOOKING_CONFIRMED,
                "Booking confirmed",
                "Your booking has been confirmed.",
                event.getBookingId());
    }

    @EventListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        if (event.isCancelledByAdmin()) {
            send(event.getArtistId(), NotificationType.BOOKING_CANCELLED,
                    "Booking cancelled",
                    "Your booking was cancelled by an administrator.",
                    event.getBookingId());
            bookingOwner(event.getStudioId()).ifPresent(owner ->
                    send(owner.getId(), NotificationType.BOOKING_CANCELLED,
                            "Booking cancelled",
                            "A booking was cancelled by an administrator.",
                            event.getBookingId()));
            return;
        }

        if (event.isCancelledByArtist()) {
            bookingOwner(event.getStudioId()).ifPresent(owner ->
                    send(owner.getId(), NotificationType.BOOKING_CANCELLED,
                            "Booking cancelled",
                            "An artist cancelled their booking.",
                            event.getBookingId()));
            return;
        }

        send(event.getArtistId(), NotificationType.BOOKING_CANCELLED,
                "Booking cancelled",
                "Your booking was cancelled by the studio.",
                event.getBookingId());
    }

    @EventListener
    public void onBookingExpired(BookingExpiredEvent event) {
        send(event.getArtistId(), NotificationType.BOOKING_EXPIRED,
                "Booking expired",
                "Your booking expired because payment was not completed in time.",
                event.getBookingId());
        bookingOwner(event.getStudioId()).ifPresent(owner ->
                send(owner.getId(), NotificationType.BOOKING_EXPIRED,
                        "Booking expired",
                        "A booking expired because payment was not completed in time.",
                        event.getBookingId()));
    }

    private java.util.Optional<User> bookingOwner(String studioId) {
        if (studioId == null) {
            return java.util.Optional.empty();
        }
        return studioRepository.findById(studioId)
                .map(s -> s.getOwnerId())
                .flatMap(userRepository::findById);
    }

    private void send(Integer userId, NotificationType type, String title, String message, String relatedEntityId) {
        if (userId == null) {
            return;
        }
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setTitle(title);
        request.setMessage(message);
        request.setRelatedEntityId(relatedEntityId);
        notificationService.createNotification(request);
    }
}

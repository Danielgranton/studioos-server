package com.studioos.server.booking;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.booking.dto.BookingResponse;
import com.studioos.server.booking.dto.ConfirmBookingRequest;
import com.studioos.server.booking.dto.CreateBookingRequest;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl {

    private final BookingRepository bookingRepository;
    private final StudioRepository studioRepository;

    // ─── Create booking (ARTIST only) ───
    @Transactional
    public BookingResponse createBooking(User currentUser, CreateBookingRequest request) {
        if (currentUser.getRole() != Role.ARTIST && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Only artists can create bookings");
        }

        // Verify studio exists
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));

        // Check availability (no conflicting bookings)
        LocalDateTime endDate = request.getSessionDate().plusHours(request.getDurationHours());
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                request.getStudioId(),
                request.getSessionDate(),
                endDate
        );
        if (!conflicts.isEmpty()) {
            throw StudioosException.badRequest("Studio is not available for the requested time slot");
        }

        Booking booking = Booking.builder()
        .studioId(request.getStudioId())
        .artistId(currentUser.getId())
        .sessionDate(request.getSessionDate())
        .durationHours(request.getDurationHours())
        .notes(request.getNotes())
        .status(BookingStatus.PENDING)
        .paymentStatus(BookingPaymentStatus.BOOKED)  // ← BOOKED not UNPAID
        .build();

        bookingRepository.save(booking);
        log.info("Booking created: {} by artist: {}", booking.getId(), currentUser.getEmail());
        return toResponse(booking, studio, currentUser);
    }

    // ─── Confirm booking (PRODUCER/STUDIO OWNER only) ───
    @Transactional
    public BookingResponse confirmBooking(User currentUser, String bookingId, ConfirmBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> StudioosException.notFound("Booking not found"));

        Studio studio = studioRepository.findById(booking.getStudioId())
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));

        // Verify ownership
        if (!studio.getOwnerId().equals(currentUser.getId()) && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You do not own this studio");
        }

            if (booking.getStatus() != BookingStatus.PENDING) {
            throw StudioosException.badRequest("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.APPROVED);  // ← APPROVED not CONFIRMED
        booking.setTotalPrice(request.getTotalPrice());
        bookingRepository.save(booking);
        log.info("Booking confirmed: {}", bookingId);
        return toResponse(booking, studio, currentUser);
    }

    // ─── Cancel booking ───
    @Transactional
    public void cancelBooking(User currentUser, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> StudioosException.notFound("Booking not found"));

        // Artist can cancel their own booking, Producer can cancel bookings for their studio
        boolean isArtist = booking.getArtistId().equals(currentUser.getId());
        Studio studio = studioRepository.findById(booking.getStudioId()).orElse(null);
        boolean isStudioOwner = studio != null && studio.getOwnerId().equals(currentUser.getId());

        if (!isArtist && !isStudioOwner && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You cannot cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw StudioosException.badRequest("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking cancelled: {}", bookingId);
    }

    // ─── Get booking details ───
    public BookingResponse getBooking(User currentUser, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> StudioosException.notFound("Booking not found"));

        // User can view their own bookings or studio owner can view bookings for their studio
        boolean isArtist = booking.getArtistId().equals(currentUser.getId());
        Studio studio = studioRepository.findById(booking.getStudioId()).orElse(null);
        boolean isStudioOwner = studio != null && studio.getOwnerId().equals(currentUser.getId());

        if (!isArtist && !isStudioOwner && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You cannot view this booking");
        }

        return toResponse(booking, studio, currentUser);
    }

    // ─── Get my bookings (as artist) ───
    public PageResponse<BookingResponse> getMyBookings(User currentUser, int page, int size) {
        if (currentUser.getRole() != Role.ARTIST && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Only artists have bookings");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sessionDate").descending());
        return PageResponse.from(
                bookingRepository.findByArtistId(currentUser.getId(), pageable)
                        .map(b -> {
                            Studio studio = studioRepository.findById(b.getStudioId()).orElse(null);
                            return toResponse(b, studio, currentUser);
                        })
        );
    }

    // ─── Get studio bookings (as studio owner) ───
    public PageResponse<BookingResponse> getStudioBookings(User currentUser, String studioId, int page, int size) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));

        if (!studio.getOwnerId().equals(currentUser.getId()) && currentUser.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You do not own this studio");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sessionDate").descending());
        return PageResponse.from(
                bookingRepository.findByStudioId(studioId, pageable)
                        .map(b -> toResponse(b, studio, currentUser))
        );
    }

    // ─── Helper ───
    private BookingResponse toResponse(Booking booking, Studio studio, User currentUser) {
        User artist = null;
        if (booking.getArtist() != null) {
            artist = booking.getArtist();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .studioId(booking.getStudioId())
                .studioName(studio != null ? studio.getStudioName() : null)
                .artistId(booking.getArtistId())
                .artistName(artist != null ? artist.getName() : null)
                .sessionDate(booking.getSessionDate())
                .durationHours(booking.getDurationHours())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
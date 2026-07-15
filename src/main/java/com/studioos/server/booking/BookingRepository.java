package com.studioos.server.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // Find by studio owner
    Page<Booking> findByStudioId(String studioId, Pageable pageable);
    boolean existsByStudioId(String studioId);

    // Find by artist
    Page<Booking> findByArtistId(Integer artistId, Pageable pageable);

    // Find bookings for a studio within a date range (for availability check)
    @Query("SELECT b FROM Booking b WHERE b.studioId = :studioId AND b.status != 'CANCELLED' " +
           "AND b.sessionDate >= :startDate AND b.sessionDate < :endDate")
    List<Booking> findConflictingBookings(String studioId, LocalDateTime startDate, LocalDateTime endDate);

    // Find pending bookings (awaiting confirmation)
    List<Booking> findByStudioIdAndStatus(String studioId, BookingStatus status);

    List<Booking> findByStatusAndPaymentStatusAndCreatedAtBefore(BookingStatus status, BookingPaymentStatus paymentStatus, LocalDateTime createdAtBefore);

    Optional<Booking> findByIdAndStudioId(String bookingId, String studioId);

    Optional<Booking> findByIdAndArtistId(String bookingId, Integer artistId);

    List<Booking> findByStudioIdIn(List<String> studioIds);
    List<Booking> findByStudioIdInAndPaymentStatus(List<String> studioIds, BookingPaymentStatus paymentStatus);
}

package com.studioos.server.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // Find by studio owner
    @EntityGraph(attributePaths = "artist")
    Page<Booking> findByStudioId(String studioId, Pageable pageable);
    boolean existsByStudioId(String studioId);

    // Find by artist
    @EntityGraph(attributePaths = "artist")
    Page<Booking> findByArtistId(Integer artistId, Pageable pageable);

    // Find bookings for a studio within a date range (for availability check)
    @Query(value = "SELECT * FROM bookings b WHERE b.studio_id = :studioId " +
           "AND b.status NOT IN ('CANCELLED', 'EXPIRED') " +
           "AND b.session_date < :endDate " +
           "AND b.session_date + (b.duration_hours * INTERVAL '1 hour') > :startDate", nativeQuery = true)
    List<Booking> findConflictingBookings(String studioId, LocalDateTime startDate, LocalDateTime endDate);

    // Find pending bookings (awaiting confirmation)
    List<Booking> findByStudioIdAndStatus(String studioId, BookingStatus status);

    List<Booking> findByStatusAndPaymentStatusAndUpdatedAtBefore(BookingStatus status, BookingPaymentStatus paymentStatus, LocalDateTime updatedAtBefore);

    Optional<Booking> findByIdAndStudioId(String bookingId, String studioId);

    List<Booking> findByArtistId(Integer artistId);

    Optional<Booking> findByIdAndArtistId(String bookingId, Integer artistId);

    List<Booking> findByStudioIdIn(List<String> studioIds);
    List<Booking> findByStudioIdInAndStatus(List<String> studioIds, BookingStatus status);
    List<Booking> findByStudioIdInAndPaymentStatus(List<String> studioIds, BookingPaymentStatus paymentStatus);

    long countByArtistIdAndStatusAndPaymentStatus(Integer artistId, BookingStatus status, BookingPaymentStatus paymentStatus);

    @Query("SELECT COUNT(b) FROM Booking b JOIN b.studio s WHERE s.ownerId = :producerId AND b.status = :status AND b.paymentStatus = :paymentStatus")
    long countByProducerIdAndStatusAndPaymentStatus(Integer producerId, BookingStatus status, BookingPaymentStatus paymentStatus);

    long countByStudioIdAndStatusAndPaymentStatus(String studioId, BookingStatus status, BookingPaymentStatus paymentStatus);
}

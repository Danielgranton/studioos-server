package com.studioos.server.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.studioos.server.shared.enums.SessionStatus;

@Repository
public interface RecordingSessionRepository extends JpaRepository<RecordingSession, String> {
    Optional<RecordingSession> findByBookingId(String bookingId);
    List<RecordingSession> findByArtistId(Integer artistId);
    List<RecordingSession> findByProducerId(Integer producerId);
    List<RecordingSession> findByEngineerId(Integer engineerId);
    List<RecordingSession> findByStatus(SessionStatus status);
    List<RecordingSession> findByScheduledStartBeforeAndStatus(LocalDateTime before, SessionStatus status);
}

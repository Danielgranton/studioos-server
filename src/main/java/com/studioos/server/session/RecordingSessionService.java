package com.studioos.server.session;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.booking.Booking;
import com.studioos.server.session.dto.RecordingSessionResponse;
import com.studioos.server.session.events.SessionCancelledEvent;
import com.studioos.server.session.events.SessionCompletedEvent;
import com.studioos.server.session.events.SessionCreatedEvent;
import com.studioos.server.session.events.SessionNoShowEvent;
import com.studioos.server.session.events.SessionStatusChangedEvent;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.enums.SessionStatus;
import com.studioos.server.shared.enums.SessionTimelineAction;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecordingSessionService {

    private final RecordingSessionRepository recordingSessionRepository;
    private final SessionTimelineService timelineService;
    private final StudioRepository studioRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SessionAccessService sessionAccessService;

    @Transactional
    public RecordingSession createSession(Booking booking) {
        if (booking == null) {
            throw StudioosException.badRequest("Booking is required");
        }

        return recordingSessionRepository.findByBookingId(booking.getId())
                .orElseGet(() -> {
                    Studio studio = studioRepository.findById(booking.getStudioId())
                            .orElseThrow(() -> StudioosException.notFound("Studio not found"));

                    RecordingSession session = RecordingSession.builder()
                            .bookingId(booking.getId())
                            .studioId(booking.getStudioId())
                            .artistId(booking.getArtistId())
                            .producerId(studio.getOwnerId())
                            .scheduledStart(booking.getSessionDate())
                            .scheduledEnd(booking.getSessionDate().plusHours(booking.getDurationHours()))
                            .status(SessionStatus.WAITING)
                            .notes(booking.getNotes())
                            .build();
                    session = recordingSessionRepository.save(session);
                    timelineService.recordEvent(session.getId(), SessionTimelineAction.SESSION_CREATED, booking.getArtistId(),
                            "Session created from paid booking " + booking.getId());
                    applicationEventPublisher.publishEvent(SessionCreatedEvent.builder()
                            .sessionId(session.getId())
                            .bookingId(booking.getId())
                            .build());
                    return session;
                });
    }

    @Transactional
    public RecordingSession startSession(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageRecording(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.CREATED, SessionStatus.WAITING);
        session.setActualStart(session.getActualStart() == null ? LocalDateTime.now() : session.getActualStart());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.RECORDING_STARTED, currentUser.getId(), details);
        applicationEventPublisher.publishEvent(SessionStatusChangedEvent.builder()
                .sessionId(sessionId)
                .status(SessionStatus.IN_PROGRESS)
                .build());
        return session;
    }

    @Transactional
    public RecordingSession pauseSession(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageRecording(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.IN_PROGRESS);
        session.setStatus(SessionStatus.PAUSED);
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.RECORDING_PAUSED, currentUser.getId(), details);
        return session;
    }

    @Transactional
    public RecordingSession resumeSession(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageRecording(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.PAUSED);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.RECORDING_RESUMED, currentUser.getId(), details);
        return session;
    }

    @Transactional
    public RecordingSession finishRecording(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageRecording(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.IN_PROGRESS, SessionStatus.PAUSED);
        session.setStatus(SessionStatus.RECORDING_COMPLETE);
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.RECORDING_FINISHED, currentUser.getId(), details);
        return session;
    }

    @Transactional
    public RecordingSession startMixing(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageProduction(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.RECORDING_COMPLETE, SessionStatus.MIXING);
        session.setStatus(SessionStatus.MIXING);
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.MIXING_STARTED, currentUser.getId(), details);
        return session;
    }

    @Transactional
    public RecordingSession finishMixing(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageProduction(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.MIXING);
        timelineService.recordEvent(sessionId, SessionTimelineAction.MIXING_FINISHED, currentUser.getId(), details);
        return session;
    }

    @Transactional
    public RecordingSession startMastering(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageProduction(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.MIXING, SessionStatus.RECORDING_COMPLETE);
        session.setStatus(SessionStatus.MASTERING);
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.MASTERING_STARTED, currentUser.getId(), details);
        return session;
    }

    @Transactional
    public RecordingSession completeSession(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanManageProduction(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.MASTERING, SessionStatus.REVIEW);
        session.setStatus(SessionStatus.COMPLETED);
        session.setActualEnd(LocalDateTime.now());
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.SESSION_COMPLETED, currentUser.getId(), details);
        applicationEventPublisher.publishEvent(SessionCompletedEvent.builder()
                .sessionId(sessionId)
                .bookingId(session.getBookingId())
                .build());
        applicationEventPublisher.publishEvent(SessionStatusChangedEvent.builder()
                .sessionId(sessionId)
                .status(SessionStatus.COMPLETED)
                .build());
        return session;
    }

    @Transactional
    public RecordingSession cancelSession(User currentUser, String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanCancel(session, currentUser);
        sessionAccessService.assertStatus(session, SessionStatus.CREATED, SessionStatus.WAITING, SessionStatus.IN_PROGRESS,
                SessionStatus.PAUSED, SessionStatus.RECORDING_COMPLETE, SessionStatus.MIXING, SessionStatus.MASTERING,
                SessionStatus.REVIEW);
        session.setStatus(SessionStatus.CANCELLED);
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.SESSION_CANCELLED, currentUser.getId(), details);
        applicationEventPublisher.publishEvent(SessionCancelledEvent.builder()
                .sessionId(sessionId)
                .bookingId(session.getBookingId())
                .reason(details)
                .build());
        return session;
    }

    @Transactional
    public RecordingSession cancelSessionByBooking(String bookingId, String details) {
        return recordingSessionRepository.findByBookingId(bookingId)
                .map(session -> cancelSessionBySessionOwner(session, details))
                .orElse(null);
    }

    private RecordingSession cancelSessionBySessionOwner(RecordingSession session, String details) {
        com.studioos.server.user.User systemUser = com.studioos.server.user.User.builder()
                .id(session.getArtistId())
                .role(Role.ARTIST)
                .build();
        return cancelSession(systemUser, session.getId(), details);
    }

    @Transactional
    public RecordingSession markNoShow(String sessionId, String details) {
        RecordingSession session = requireSession(sessionId);
        session.setStatus(SessionStatus.NO_SHOW);
        session = recordingSessionRepository.save(session);
        timelineService.recordEvent(sessionId, SessionTimelineAction.NO_SHOW_MARKED, session.getArtistId(), details);
        applicationEventPublisher.publishEvent(SessionNoShowEvent.builder()
                .sessionId(sessionId)
                .bookingId(session.getBookingId())
                .build());
        return session;
    }

    @Transactional(readOnly = true)
    public List<RecordingSessionResponse> getMySessions(User currentUser) {
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return recordingSessionRepository.findAll().stream().map(this::toResponse).toList();
        }

        return java.util.stream.Stream.of(
                        recordingSessionRepository.findByArtistId(currentUser.getId()),
                        recordingSessionRepository.findByProducerId(currentUser.getId()),
                        recordingSessionRepository.findByEngineerId(currentUser.getId()))
                .flatMap(List::stream)
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecordingSessionResponse getSession(User currentUser, String sessionId) {
        RecordingSession session = requireSession(sessionId);
        sessionAccessService.assertCanView(session, currentUser);
        return toResponse(session);
    }

    @Transactional
    public void markOverdueNoShows() {
        LocalDateTime now = LocalDateTime.now();
        recordingSessionRepository.findByScheduledStartBeforeAndStatus(now.minusMinutes(15), SessionStatus.WAITING)
                .forEach(session -> markNoShow(session.getId(), "Auto-marked as no-show after grace period"));
    }

    private RecordingSession requireSession(String sessionId) {
        return recordingSessionRepository.findById(sessionId)
                .orElseThrow(() -> StudioosException.notFound("Session not found"));
    }

    private RecordingSessionResponse toResponse(RecordingSession session) {
        return RecordingSessionResponse.builder()
                .id(session.getId())
                .bookingId(session.getBookingId())
                .studioId(session.getStudioId())
                .artistId(session.getArtistId())
                .producerId(session.getProducerId())
                .engineerId(session.getEngineerId())
                .scheduledStart(session.getScheduledStart())
                .scheduledEnd(session.getScheduledEnd())
                .actualStart(session.getActualStart())
                .actualEnd(session.getActualEnd())
                .status(session.getStatus())
                .notes(session.getNotes())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}

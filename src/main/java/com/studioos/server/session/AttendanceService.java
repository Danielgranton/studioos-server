package com.studioos.server.session;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.shared.enums.AttendanceStatus;
import com.studioos.server.shared.enums.SessionTimelineAction;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final SessionAttendanceRepository sessionAttendanceRepository;
    private final SessionTimelineService timelineService;

    @Transactional
    public SessionAttendance checkIn(String sessionId, Integer userId, String role, boolean late) {
        SessionAttendance attendance = sessionAttendanceRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElse(SessionAttendance.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .role(role)
                        .build());
        attendance.setJoinedAt(LocalDateTime.now());
        attendance.setStatus(late ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        attendance = sessionAttendanceRepository.save(attendance);
        timelineService.recordEvent(sessionId,
                "ARTIST".equalsIgnoreCase(role) ? SessionTimelineAction.ARTIST_CHECKED_IN
                        : "PRODUCER".equalsIgnoreCase(role) ? SessionTimelineAction.PRODUCER_CHECKED_IN
                        : SessionTimelineAction.ENGINEER_CHECKED_IN,
                userId,
                "Checked in as " + role);
        return attendance;
    }

    @Transactional
    public SessionAttendance checkOut(String sessionId, Integer userId, String details) {
        SessionAttendance attendance = sessionAttendanceRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance record not found"));
        attendance.setLeftAt(LocalDateTime.now());
        attendance = sessionAttendanceRepository.save(attendance);
        timelineService.recordEvent(sessionId, SessionTimelineAction.CHECKED_OUT, userId, details);
        return attendance;
    }

    @Transactional
    public SessionAttendance markNoShow(String sessionId, Integer userId, String role, String details) {
        SessionAttendance attendance = sessionAttendanceRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElse(SessionAttendance.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .role(role)
                        .build());
        attendance.setStatus(AttendanceStatus.NO_SHOW);
        attendance = sessionAttendanceRepository.save(attendance);
        timelineService.recordEvent(sessionId, SessionTimelineAction.NO_SHOW_MARKED, userId, details);
        return attendance;
    }

    @Transactional(readOnly = true)
    public List<SessionAttendance> getAttendance(String sessionId) {
        return sessionAttendanceRepository.findBySessionId(sessionId);
    }
}

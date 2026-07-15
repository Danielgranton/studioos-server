package com.studioos.server.session;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.shared.enums.SessionTimelineAction;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionTimelineService {

    private final SessionTimelineRepository sessionTimelineRepository;

    @Transactional
    public SessionTimelineEntry recordEvent(String sessionId, SessionTimelineAction action, Integer performedBy, String details) {
        return sessionTimelineRepository.save(SessionTimelineEntry.builder()
                .sessionId(sessionId)
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .build());
    }

    @Transactional(readOnly = true)
    public List<SessionTimelineEntry> getTimeline(String sessionId) {
        return sessionTimelineRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}

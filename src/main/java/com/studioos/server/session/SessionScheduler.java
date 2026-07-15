package com.studioos.server.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionScheduler {

    private final RecordingSessionService recordingSessionService;

    @Scheduled(fixedDelay = 60_000L)
    public void markOverdueSessions() {
        log.debug("Running session overdue no-show sweep");
        recordingSessionService.markOverdueNoShows();
    }
}

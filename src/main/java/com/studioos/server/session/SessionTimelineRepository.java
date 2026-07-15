package com.studioos.server.session;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionTimelineRepository extends JpaRepository<SessionTimelineEntry, String> {
    List<SessionTimelineEntry> findBySessionIdOrderByTimestampAsc(String sessionId);
}

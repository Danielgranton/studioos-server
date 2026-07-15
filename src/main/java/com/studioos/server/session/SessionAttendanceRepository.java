package com.studioos.server.session;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionAttendanceRepository extends JpaRepository<SessionAttendance, String> {
    Optional<SessionAttendance> findBySessionIdAndUserId(String sessionId, Integer userId);
    List<SessionAttendance> findBySessionId(String sessionId);
}

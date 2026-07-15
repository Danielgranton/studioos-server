package com.studioos.server.session;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionDeliverableRepository extends JpaRepository<SessionDeliverable, String> {
    List<SessionDeliverable> findBySessionId(String sessionId);
}

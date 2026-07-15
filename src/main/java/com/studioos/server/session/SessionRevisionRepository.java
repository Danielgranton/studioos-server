package com.studioos.server.session;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRevisionRepository extends JpaRepository<SessionRevision, String> {
    List<SessionRevision> findBySessionId(String sessionId);
    List<SessionRevision> findByDeliverableId(String deliverableId);
}

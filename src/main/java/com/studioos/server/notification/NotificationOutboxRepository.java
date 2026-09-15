package com.studioos.server.notification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, String> {

    @Query(value = """
            SELECT *
            FROM notification_outbox
            WHERE status IN (:statuses)
              AND next_attempt_at <= :now
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<NotificationOutbox> claimReady(
            @Param("statuses") Collection<String> statuses,
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );
}

package com.studioos.server.engagement;

import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EngagementEdgeRepository extends JpaRepository<EngagementEdge, String> {
    Optional<EngagementEdge> findByActorIdAndTargetTypeAndTargetIdAndAction(
            Integer actorId, EngagementTargetType targetType, String targetId, EngagementAction action);

    long countByTargetTypeAndTargetIdAndAction(
            EngagementTargetType targetType, String targetId, EngagementAction action);

    long countByTargetTypeAndTargetIdAndActionAndCreatedAtAfter(
            EngagementTargetType targetType, String targetId, EngagementAction action, LocalDateTime after);

    @Query("SELECT COUNT(e) FROM EngagementEdge e WHERE e.actorId = :actorId AND e.action = :action")
    long countByActorAndAction(@Param("actorId") Integer actorId, @Param("action") EngagementAction action);
}

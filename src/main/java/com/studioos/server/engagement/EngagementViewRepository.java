package com.studioos.server.engagement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface EngagementViewRepository extends JpaRepository<EngagementView, String> {
    long countByTargetTypeAndTargetId(EngagementTargetType targetType, String targetId);

    long countByTargetTypeAndTargetIdAndCreatedAtAfter(EngagementTargetType targetType, String targetId, LocalDateTime after);
}

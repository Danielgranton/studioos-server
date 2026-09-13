package com.studioos.server.engagement;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "engagement_edges", uniqueConstraints = @UniqueConstraint(columnNames = {"actor_id", "target_type", "target_id", "action"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "actor_id", nullable = false)
    private Integer actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private EngagementTargetType targetType;

    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EngagementAction action;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

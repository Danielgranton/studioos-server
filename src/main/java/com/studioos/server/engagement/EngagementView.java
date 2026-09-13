package com.studioos.server.engagement;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "engagement_views")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Integer viewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private EngagementTargetType targetType;

    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

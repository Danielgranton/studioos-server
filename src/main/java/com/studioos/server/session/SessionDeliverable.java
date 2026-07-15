package com.studioos.server.session;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.DeliverableType;
import com.studioos.server.shared.enums.DeliverableStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "session_deliverables")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliverableType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliverableStatus status;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String objectKey;

    private String contentType;

    private String originalFileId;
    private String previewFileId;
    private String thumbnailId;

    private Integer duration;

    @Column(nullable = false)
    private Integer uploadedBy;

    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

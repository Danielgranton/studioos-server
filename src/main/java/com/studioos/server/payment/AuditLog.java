package com.studioos.server.payment;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.AuditEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    // The id of the entity this log refers to (transaction/wallet/escrow/withdrawal)
    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String entityType;

    private Integer userId;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ─── Audit ───
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
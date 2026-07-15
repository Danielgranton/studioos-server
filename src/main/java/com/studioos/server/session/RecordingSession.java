package com.studioos.server.session;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.SessionStatus;

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
@Table(name = "recording_sessions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String bookingId;

    @Column(nullable = false)
    private String studioId;

    @Column(nullable = false)
    private Integer artistId;

    private Integer producerId;
    private Integer engineerId;

    @Column(nullable = false)
    private LocalDateTime scheduledStart;

    @Column(nullable = false)
    private LocalDateTime scheduledEnd;

    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

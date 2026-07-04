package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.MediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media_processing_jobs")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String beatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatId", insertable = false, updatable = false)
    private Beat beat;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaJobOperation operation;

    @Column(nullable = false)
    private String externalJobId;   // jobId returned by the Media Service

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaJobStatus status;

    private String resultReference;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
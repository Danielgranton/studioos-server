package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.studioos.server.shared.enums.AdMediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ad_media_processing_jobs")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdMediaProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String advertisementId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AdMediaJobOperation operation;

    @Column(nullable = false)
    private String externalJobId;

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
package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.studioos.server.shared.enums.UploadSessionStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ad_upload_sessions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdUploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Integer advertiserId;

    @Column(nullable = false)
    private String advertisementId;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String objectKey;

    private String contentType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadSessionStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.UploadFileType;
import com.studioos.server.shared.enums.UploadSessionStatus;
import com.studioos.server.user.User;

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
@Table(name = "upload_sessions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Integer producerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producerId", insertable = false, updatable = false)
    private User producer;

    private String beatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatId", insertable = false, updatable = false)
    private Beat beat;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadFileType fileType;

    private String contentType;
    private String checksum;
    private Long sizeBytes;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadSessionStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
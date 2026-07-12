package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdCreativeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "advertisements")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String campaignId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AdCreativeType type;

    @Column(nullable = false)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String ctaText;
    private String ctaUrl;

    private String mediaUrl;       // object key, null until processing completes
    private String thumbnailUrl;   // object key, video/image only
    private Integer duration;      // seconds, video/audio only

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AdCreativeStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
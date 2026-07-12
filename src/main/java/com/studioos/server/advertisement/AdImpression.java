package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.studioos.server.shared.enums.AdPlacement;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ad_impressions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String advertisementId;

    @Column(nullable = false)
    private String campaignId;

    private Integer userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AdPlacement placement;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
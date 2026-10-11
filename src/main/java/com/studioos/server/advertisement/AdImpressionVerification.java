package com.studioos.server.advertisement;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.shared.enums.AdImpressionVerificationStatus;
import com.studioos.server.shared.enums.AdPlacement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ad_impression_verifications")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdImpressionVerification {

    @Id
    @Column(length = 36)
    private String token;

    @Column(nullable = false, length = 36)
    private String advertisementId;

    @Column(nullable = false, length = 36)
    private String campaignId;

    private Integer userId;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AdPlacement placement;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AdImpressionVerificationStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime confirmedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

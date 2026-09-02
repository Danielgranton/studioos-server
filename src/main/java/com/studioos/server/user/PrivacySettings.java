package com.studioos.server.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "privacy_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettings {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(nullable = false)
    @Builder.Default
    private boolean profileDiscoverable = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVisible = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean phoneVisible = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean directMessagesEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean personalizedRecommendations = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

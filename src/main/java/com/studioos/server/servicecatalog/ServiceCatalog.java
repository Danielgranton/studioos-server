package com.studioos.server.servicecatalog;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "service_catalog")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(nullable = false, unique = true, length = 140)
    private String name;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(name = "artist_allowed", nullable = false)
    @Builder.Default
    private boolean artistAllowed = true;

    @Column(name = "studio_allowed", nullable = false)
    @Builder.Default
    private boolean studioAllowed = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by")
    private Integer createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

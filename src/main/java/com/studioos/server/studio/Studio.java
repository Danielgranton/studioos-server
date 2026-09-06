package com.studioos.server.studio;

import com.studioos.server.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "studios")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Studio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String studioName;

    @Column(nullable = false)
    private String location;

    private Integer pricing;

    @Column(nullable = false)
    private String availability;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String badge;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> genres = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> equipment = new ArrayList<>();

    private Integer rooms;
    private Integer yearsActive;
    private String responseTime;
    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;
    private String nextAvailable;
    @Column(nullable = false)
    @Builder.Default
    private Integer bookings = 0;
    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    private String profileImage;
    private String profileImageLarge;
    private String profileImageMedium;
    private String profileImageThumbnail;

    // ─── Owner ───
    @Column(nullable = false)
    private Integer ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", insertable = false, updatable = false)
    private User owner;

    // ─── Services ───
    @OneToMany(mappedBy = "studio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudioService> services = new ArrayList<>();

    // ─── Ratings ───
    @OneToMany(mappedBy = "studio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudioRating> ratings = new ArrayList<>();

    // ─── Audit ───
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

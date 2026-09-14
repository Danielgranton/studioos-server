package com.studioos.server.artist;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.studioos.server.booking.Booking;
import com.studioos.server.reviews.ReviewModerationStatus;
import com.studioos.server.user.User;

@Entity
@Table(name = "artist_reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"reviewer_id", "artist_id"}))
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "reviewer_id", nullable = false)
    private Integer reviewerId;

    @Column(name = "artist_id", nullable = false)
    private Integer artistId;

    @Column(name = "booking_id", nullable = false)
    private String bookingId;

    @Column(nullable = false)
    private Float rating;

    @Column(columnDefinition = "TEXT")
    private String review;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewModerationStatus moderationStatus = ReviewModerationStatus.ACTIVE;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "moderated_by")
    private Integer moderatedBy;

    @Column(name = "moderation_reason", columnDefinition = "TEXT")
    private String moderationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", insertable = false, updatable = false)
    private User artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", insertable = false, updatable = false)
    private Booking booking;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

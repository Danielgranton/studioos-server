package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studioos.server.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "beat_play_history")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeatPlayHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Integer userId;   // nullable — anonymous plays counted

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;

    @Column(nullable = false)
    private String beatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatId", insertable = false, updatable = false)
    private Beat beat;

    private Integer durationPlayed;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime playedAt;
}
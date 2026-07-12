package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;
import com.studioos.server.studio.Studio;
import com.studioos.server.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "beats")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Integer producerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producerId", insertable = false, updatable = false)
    private User producer;

    private String studioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studioId", insertable = false, updatable = false)
    private Studio studio;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String genreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genreId", insertable = false, updatable = false)
    private BeatGenre genre;

    private Integer bpm;
    private String keySignature;
    private String mood;

    private String coverUrl;
    private String thumbnailUrl;
    private String audioUrl;
    private String previewUrl;
    private String waveformUrl;
    private Integer duration;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BeatStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BeatVisibility visibility;

    @Builder.Default
    private Boolean exclusiveSold = false;

    @Builder.Default
    private Integer playCount = 0;

    @Builder.Default
    private Integer downloadCount = 0;

    @Builder.Default
    private Integer likeCount = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

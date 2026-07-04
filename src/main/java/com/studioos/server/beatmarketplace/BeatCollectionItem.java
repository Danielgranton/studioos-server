package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "beat_collection_items")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeatCollectionItem {

    @EmbeddedId
    private BeatCollectionItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("collectionId")
    @JoinColumn(name = "collectionId")
    private BeatCollection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("beatId")
    @JoinColumn(name = "beatId")
    private Beat beat;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;
}
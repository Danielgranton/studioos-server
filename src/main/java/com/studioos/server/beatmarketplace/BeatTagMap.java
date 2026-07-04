package com.studioos.server.beatmarketplace;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "beat_tag_map")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeatTagMap {

    @EmbeddedId
    private BeatTagMapId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("beatId")
    @JoinColumn(name = "beatId")
    private Beat beat;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tagId")
    private BeatTag tag;
}
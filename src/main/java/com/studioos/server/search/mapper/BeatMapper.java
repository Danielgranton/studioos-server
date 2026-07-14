package com.studioos.server.search.mapper;

import com.studioos.server.beatmarketplace.Beat;
import com.studioos.server.search.document.BeatDocument;
import com.studioos.server.search.dto.BeatSearchResult;

public final class BeatMapper {
    private BeatMapper() {
    }

    public static BeatDocument toDocument(Beat beat, Integer price) {
        return BeatDocument.builder()
                .id(beat.getId())
                .title(beat.getTitle())
                .genre(beat.getGenre() != null ? beat.getGenre().getName() : null)
                .bpm(beat.getBpm())
                .keySignature(beat.getKeySignature())
                .mood(beat.getMood())
                .producerId(beat.getProducerId())
                .studioId(beat.getStudioId())
                .price(price)
                .playCount(beat.getPlayCount())
                .likeCount(beat.getLikeCount())
                .status(beat.getStatus().name())
                .createdAt(beat.getCreatedAt() != null ? beat.getCreatedAt().toString() : null)
                .build();
    }

    public static BeatSearchResult toResult(BeatDocument doc, Double score) {
        return BeatSearchResult.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .genre(doc.getGenre())
                .bpm(doc.getBpm())
                .price(doc.getPrice())
                .playCount(doc.getPlayCount())
                .likeCount(doc.getLikeCount())
                .score(score)
                .build();
    }
}

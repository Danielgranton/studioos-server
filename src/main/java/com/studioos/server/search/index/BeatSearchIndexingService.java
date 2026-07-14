package com.studioos.server.search.index;

import com.studioos.server.beatmarketplace.Beat;
import com.studioos.server.beatmarketplace.BeatLicenseRepository;
import com.studioos.server.search.document.BeatDocument;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeatSearchIndexingService {

    private static final String INDEX_NAME = "beats";

    private final OpenSearchClient openSearchClient;
    private final BeatLicenseRepository beatLicenseRepository;

    public void indexBeat(Beat beat) {
        try {
            Integer cheapestPrice = beatLicenseRepository.findByBeatIdAndActiveTrue(beat.getId()).stream()
                    .map(l -> l.getPrice())
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            BeatDocument doc = BeatDocument.builder()
                    .id(beat.getId())
                    .title(beat.getTitle())
                    .genre(beat.getGenre() != null ? beat.getGenre().getName() : null)
                    .bpm(beat.getBpm())
                    .keySignature(beat.getKeySignature())
                    .mood(beat.getMood())
                    .producerId(beat.getProducerId())
                    .studioId(beat.getStudioId())
                    .price(cheapestPrice)
                    .playCount(beat.getPlayCount())
                    .likeCount(beat.getLikeCount())
                    .status(beat.getStatus().name())
                    .createdAt(beat.getCreatedAt() != null ? beat.getCreatedAt().toString() : null)
                    .build();

            openSearchClient.index(i -> i.index(INDEX_NAME).id(beat.getId()).document(doc));
        } catch (Exception e) {
            // Indexing failure must never block the beat's actual READY transition —
            // same fail-open posture as notification failures elsewhere in this codebase.
            log.error("Failed to index beat {} in OpenSearch: {}", beat.getId(), e.getMessage());
        }
    }
}
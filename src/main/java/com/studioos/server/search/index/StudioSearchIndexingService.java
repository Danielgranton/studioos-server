package com.studioos.server.search.index;

import com.studioos.server.search.document.StudioDocument;
import com.studioos.server.studio.Studio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudioSearchIndexingService {

    private static final String INDEX_NAME = "studios";

    private final OpenSearchClient openSearchClient;

    public void indexStudio(Studio studio) {
        try {
            double avgRating = studio.getRatings() == null || studio.getRatings().isEmpty()
                    ? 0.0
                    : studio.getRatings().stream()
                            .mapToDouble(r -> r.getRating())
                            .average()
                            .orElse(0.0);

            StudioDocument doc = StudioDocument.builder()
                    .id(studio.getId())
                    .studioName(studio.getStudioName())
                    .location(studio.getLocation())
                    .description(studio.getDescription())
                    .pricing(studio.getPricing())
                    .ownerId(studio.getOwnerId())
                    .averageRating(avgRating)
                    .ratingCount(studio.getRatings() != null ? studio.getRatings().size() : 0)
                    .build();

            openSearchClient.index(i -> i.index(INDEX_NAME).id(studio.getId()).document(doc));
        } catch (Exception e) {
            log.error("Failed to index studio {} in OpenSearch: {}", studio.getId(), e.getMessage());
        }
    }
}

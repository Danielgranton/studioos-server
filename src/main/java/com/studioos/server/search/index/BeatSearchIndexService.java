package com.studioos.server.search.index;

import jakarta.annotation.PostConstruct;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeatSearchIndexService {

    private static final String BEAT_INDEX = "beats";
    private static final String STUDIO_INDEX = "studios";

    private final OpenSearchClient openSearchClient;

    @PostConstruct
    public void ensureIndicesExist() {
        ensureIndexExists(BEAT_INDEX);
        ensureIndexExists(STUDIO_INDEX);
    }

    private void ensureIndexExists(String indexName) {
        try {
            boolean exists = openSearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(indexName)))
                    .value();

            if (!exists) {
                openSearchClient.indices().create(SearchIndexMappings.createIndexRequest(indexName));
                log.info("Created OpenSearch index: {}", indexName);
            }
        } catch (Exception e) {
            log.error("Failed to verify/create OpenSearch index '{}': {}", indexName, e.getMessage());
        }
    }
}

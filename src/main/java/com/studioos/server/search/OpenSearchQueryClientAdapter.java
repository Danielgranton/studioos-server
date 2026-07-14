package com.studioos.server.search;

import java.io.IOException;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.util.ObjectBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenSearchQueryClientAdapter implements OpenSearchQueryClient {

    private final OpenSearchClient openSearchClient;

    @Override
    public <TDocument> SearchResponse<TDocument> search(
            Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn,
            Class<TDocument> tDocumentClass) throws IOException {
        return openSearchClient.search(fn, tDocumentClass);
    }
}

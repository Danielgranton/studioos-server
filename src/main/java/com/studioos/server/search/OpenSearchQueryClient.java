package com.studioos.server.search;

import java.io.IOException;
import java.util.function.Function;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.util.ObjectBuilder;

public interface OpenSearchQueryClient {
    <TDocument> SearchResponse<TDocument> search(
            Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn,
            Class<TDocument> tDocumentClass) throws IOException;
}

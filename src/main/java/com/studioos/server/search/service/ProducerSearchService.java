package com.studioos.server.search.service;

import com.studioos.server.search.OpenSearchQueryClient;
import com.studioos.server.search.document.ProducerDocument;
import com.studioos.server.search.dto.ProducerSearchResult;
import com.studioos.server.search.dto.SearchPageResponse;
import com.studioos.server.search.mapper.ProducerMapper;
import com.studioos.server.search.exception.SearchException;
import com.studioos.server.search.util.SearchSanitizer;
import java.util.List;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProducerSearchService {

    private final OpenSearchQueryClient openSearchClient;

    public SearchPageResponse<ProducerSearchResult> search(String query, int page, int size) {
        String needle = SearchSanitizer.sanitize(query);
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            if (!needle.isBlank()) {
            boolQuery.should(s -> s.match(m -> m.field("name").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("genre").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("location").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("bio").query(q -> q.stringValue(needle))))
                    .minimumShouldMatch("1");
            }
            Query finalQuery = Query.of(q -> q.bool(boolQuery.build()));
            SearchResponse<ProducerDocument> response = openSearchClient.search(s -> s
                        .index("producers").query(finalQuery).from(page * size).size(size),
                ProducerDocument.class);
            List<ProducerSearchResult> results = response.hits().hits().stream()
                .filter(hit -> hit.source() != null)
                .map(hit -> ProducerMapper.toResult(hit.source(), hit.score()))
                .toList();
            long total = response.hits().total() == null ? results.size() : response.hits().total().value();
            return SearchPageResponse.<ProducerSearchResult>builder()
                    .results(results).page(page).size(size).total(total).build();
        } catch (Exception e) {
            throw new SearchException("Producer search is temporarily unavailable", e);
        }
    }
}

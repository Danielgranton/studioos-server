package com.studioos.server.search.service;

import com.studioos.server.search.OpenSearchQueryClient;
import com.studioos.server.search.document.AdvertisementDocument;
import com.studioos.server.search.dto.AdvertisementSearchResult;
import com.studioos.server.search.dto.SearchPageResponse;
import com.studioos.server.search.mapper.AdvertisementMapper;
import com.studioos.server.search.exception.SearchException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdvertisementSearchService {

    private final OpenSearchQueryClient openSearchClient;

    private static final List<FieldValue> VISIBLE_STATUSES = List.of(
            FieldValue.of(v -> v.stringValue("READY")),
            FieldValue.of(v -> v.stringValue("RUNNING")),
            FieldValue.of(v -> v.stringValue("PENDING_REVIEW")));

    public SearchPageResponse<AdvertisementSearchResult> search(String query, int page, int size) {
        String needle = query == null ? "" : query.trim();
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            boolQuery.filter(f -> f.terms(t -> t.field("status").terms(v -> v.value(VISIBLE_STATUSES))));
            if (!needle.isBlank()) {
            boolQuery.should(s -> s.match(m -> m.field("headline").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("description").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("campaignTitle").query(q -> q.stringValue(needle))))
                    .minimumShouldMatch("1");
            }
            Query finalQuery = Query.of(q -> q.bool(boolQuery.build()));
            SearchResponse<AdvertisementDocument> response = openSearchClient.search(s -> s
                        .index("advertisements").query(finalQuery).from(page * size).size(size),
                AdvertisementDocument.class);
            List<AdvertisementSearchResult> results = response.hits().hits().stream()
                .filter(hit -> hit.source() != null)
                .map(hit -> AdvertisementMapper.toResult(hit.source(), hit.score()))
                .toList();
            long total = response.hits().total() == null ? results.size() : response.hits().total().value();
            return SearchPageResponse.<AdvertisementSearchResult>builder()
                    .results(results).page(page).size(size).total(total).build();
        } catch (Exception e) {
            throw new SearchException("Advertisement search is temporarily unavailable", e);
        }
    }
}

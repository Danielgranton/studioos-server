package com.studioos.server.search;

import com.studioos.server.search.analytics.SearchAnalyticsService;
import com.studioos.server.search.document.StudioDocument;
import com.studioos.server.search.dto.StudioSearchRequest;
import com.studioos.server.search.dto.StudioSearchResult;
import com.studioos.server.shared.enums.SearchEntityType;
import com.studioos.server.user.User;

import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchStudioSearchService {

    private static final String INDEX_NAME = "studios";

    private final OpenSearchQueryClient openSearchClient;
    private final SearchAnalyticsService searchAnalyticsService;

    public List<StudioSearchResult> searchStudios(StudioSearchRequest request) {

        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                boolQuery.must(m -> m.match(mt -> mt.field("studioName").query(q -> q.stringValue(request.getQuery()))));
            }
            if (request.getLocation() != null) {
                boolQuery.filter(f -> f.term(t -> t.field("location").value(v -> v.stringValue(request.getLocation()))));
            }

            Query finalQuery = Query.of(q -> q.bool(boolQuery.build()));

            SearchResponse<StudioDocument> response = openSearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(finalQuery)
                    .from(request.getPage() * request.getSize())
                    .size(request.getSize()),
                    StudioDocument.class);

            List<StudioSearchResult> results = response.hits().hits().stream()
                    .map(hit -> toResult(hit.source(), hit.score()))
                    .collect(Collectors.toList());

            searchAnalyticsService.recordSearch(SearchEntityType.STUDIO, request.getQuery(), currentUserId(), results.size());

            return results;
        } catch (Exception e) {
            log.error("OpenSearch studio query failed: {}", e.getMessage());
            return List.of();
        }
    }

    private StudioSearchResult toResult(StudioDocument doc, Double score) {
        return StudioSearchResult.builder()
                .id(doc.getId())
                .studioName(doc.getStudioName())
                .location(doc.getLocation())
                .pricing(doc.getPricing())
                .averageRating(doc.getAverageRating())
                .score(score)
                .build();
    }

    private Integer currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}

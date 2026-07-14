package com.studioos.server.search;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.server.search.analytics.SearchAnalyticsService;
import com.studioos.server.search.document.BeatDocument;
import com.studioos.server.search.dto.AutocompleteSuggestion;
import com.studioos.server.search.dto.BeatSearchRequest;
import com.studioos.server.search.dto.BeatSearchResult;
import com.studioos.server.search.ranking.BeatRankingEngine;
import com.studioos.server.shared.enums.SearchEntityType;
import com.studioos.server.user.User;

import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchBeatSearchService implements SearchService {

    private static final String INDEX_NAME = "beats";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    
    private final OpenSearchQueryClient openSearchClient;
    private final StringRedisTemplate redisTemplate;
    private final BeatRankingEngine beatRankingEngine;
    private final ObjectMapper objectMapper;
    private final SearchAnalyticsService searchAnalyticsService;

    @Override
    public List<BeatSearchResult> searchBeats(BeatSearchRequest request) {

        String cacheKey = buildCacheKey(request);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return List.of(objectMapper.readValue(cached, BeatSearchResult[].class));
            } catch (Exception e) {
                log.warn("Failed to deserialize cached search result for key {}: {}", cacheKey, e.getMessage());
                // fall through to live search rather than fail the request over a cache issue
            }
        }

        List<BeatSearchResult> results = executeSearch(request);

        searchAnalyticsService.recordSearch(SearchEntityType.BEAT, request.getQuery(), currentUserId(), results.size());

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(results), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache search result for key {}: {}", cacheKey, e.getMessage());
        }

        return results;
    }

   private List<BeatSearchResult> executeSearch(BeatSearchRequest request) {
    try {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        boolQuery.must(m -> m.term(t -> t.field("status").value(v -> v.stringValue("READY"))));

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            boolQuery.must(m -> m.match(mt -> mt.field("title").query(q -> q.stringValue(request.getQuery()))));
        }
        if (request.getGenre() != null) {
            boolQuery.filter(f -> f.term(t -> t.field("genre").value(v -> v.stringValue(request.getGenre()))));
        }
        if (request.getBpmMin() != null || request.getBpmMax() != null) {
            boolQuery.filter(f -> f.range(r -> {
                r.field("bpm");
                if (request.getBpmMin() != null) r.gte(org.opensearch.client.json.JsonData.of(request.getBpmMin()));
                if (request.getBpmMax() != null) r.lte(org.opensearch.client.json.JsonData.of(request.getBpmMax()));
                return r;
            }));
        }

        Query finalQuery = Query.of(q -> q.bool(boolQuery.build()));

        SearchResponse<BeatDocument> response = openSearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(finalQuery)
                .from(request.getPage() * request.getSize())
                .size(request.getSize()),
                BeatDocument.class);

        List<Hit<BeatDocument>> hits = response.hits().hits();

        double maxTextScore = hits.stream()
                .mapToDouble(h -> h.score() != null ? h.score() : 0)
                .max().orElse(1.0);
        int maxPlayCount = hits.stream()
                .mapToInt(h -> h.source() != null && h.source().getPlayCount() != null ? h.source().getPlayCount() : 0)
                .max().orElse(1);

        return hits.stream()
                .map(hit -> {
                    BeatSearchResult result = toResult(hit.source(), hit.score());
                    result.setRankScore(beatRankingEngine.score(hit, maxTextScore, maxPlayCount));
                    return result;
                })
                .sorted(Comparator.comparingDouble(BeatSearchResult::getRankScore).reversed())
                .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("OpenSearch query failed: {}", e.getMessage());
        return List.of();
    }
    }

    public List<AutocompleteSuggestion> suggest(String prefix) {
    if (prefix == null || prefix.isBlank()) return List.of();

    try {
        SearchResponse<BeatDocument> response = openSearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q.matchPhrasePrefix(m -> m.field("title").query(prefix)))
                .size(8),
                BeatDocument.class);

        return response.hits().hits().stream()
                .filter(h -> h.source() != null)
                .map(h -> new AutocompleteSuggestion(h.source().getId(), h.source().getTitle()))
                .collect(Collectors.toList());
    } catch (Exception e) {
        log.error("Autocomplete query failed: {}", e.getMessage());
        return List.of();
    }
    }

    private BeatSearchResult toResult(BeatDocument doc, Double score) {
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

    private String buildCacheKey(BeatSearchRequest request) {
        return "search:beats:%s:%s:%s:%s:%d:%d".formatted(
                request.getQuery(), request.getGenre(), request.getBpmMin(),
                request.getBpmMax(), request.getPage(), request.getSize());
    }

    private Integer currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}

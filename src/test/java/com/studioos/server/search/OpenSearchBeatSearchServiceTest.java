package com.studioos.server.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.server.search.analytics.SearchAnalyticsService;
import com.studioos.server.search.document.BeatDocument;
import com.studioos.server.search.dto.BeatSearchRequest;
import com.studioos.server.search.dto.BeatSearchResult;
import com.studioos.server.search.ranking.BeatRankingEngine;
import com.studioos.server.shared.enums.SearchEntityType;
import com.studioos.server.user.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class OpenSearchBeatSearchServiceTest {

    @Mock
    private OpenSearchQueryClient openSearchClient;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private BeatRankingEngine beatRankingEngine;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SearchAnalyticsService searchAnalyticsService;

    @InjectMocks
    private OpenSearchBeatSearchService searchService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchBeatsRecordsAuthenticatedUserIdAndReturnsResults() throws Exception {
        BeatDocument document = BeatDocument.builder()
                .id("beat-1")
                .title("Night Ride")
                .genre("Afrobeats")
                .playCount(7)
                .build();
        Hit<BeatDocument> hit = Hit.of(h -> h.index("beats").id("beat-1").score(1.2).source(document));
        SearchResponse<BeatDocument> response = SearchResponse.searchResponseOf(r -> r
                .took(1)
                .timedOut(false)
                .shards(s -> s.total(1).successful(1).failed(0))
                .hits(h -> h
                        .total(t -> t.value(1).relation(TotalHitsRelation.Eq))
                        .maxScore(1.2)
                        .hits(hit)));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(openSearchClient.search(any(), eq(BeatDocument.class))).thenReturn(response);
        when(beatRankingEngine.score(any(), anyDouble(), anyInt())).thenReturn(0.75);
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        User user = mock(User.class);
        when(user.getId()).thenReturn(42);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "N/A", List.of()));

        BeatSearchRequest request = new BeatSearchRequest();
        request.setQuery("night");

        List<BeatSearchResult> results = searchService.searchBeats(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("beat-1");
        verify(searchAnalyticsService).recordSearch(SearchEntityType.BEAT, "night", 42, 1);
    }
}

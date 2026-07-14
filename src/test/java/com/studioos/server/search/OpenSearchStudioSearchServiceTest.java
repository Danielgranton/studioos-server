package com.studioos.server.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studioos.server.search.analytics.SearchAnalyticsService;
import com.studioos.server.search.document.StudioDocument;
import com.studioos.server.search.dto.StudioSearchRequest;
import com.studioos.server.search.dto.StudioSearchResult;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class OpenSearchStudioSearchServiceTest {

    @Mock
    private OpenSearchQueryClient openSearchClient;
    @Mock
    private SearchAnalyticsService searchAnalyticsService;

    @InjectMocks
    private OpenSearchStudioSearchService searchService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchStudiosRecordsAuthenticatedUserIdAndReturnsResults() throws Exception {
        StudioDocument document = StudioDocument.builder()
                .id("studio-1")
                .studioName("Studio One")
                .location("Nairobi")
                .pricing(5000)
                .averageRating(4.9)
                .build();
        Hit<StudioDocument> hit = Hit.of(h -> h.index("studios").id("studio-1").score(1.1).source(document));
        SearchResponse<StudioDocument> response = SearchResponse.searchResponseOf(r -> r
                .took(1)
                .timedOut(false)
                .shards(s -> s.total(1).successful(1).failed(0))
                .hits(h -> h
                        .total(t -> t.value(1).relation(TotalHitsRelation.Eq))
                        .maxScore(1.1)
                        .hits(hit)));

        when(openSearchClient.search(any(), eq(StudioDocument.class))).thenReturn(response);

        User user = mock(User.class);
        when(user.getId()).thenReturn(77);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "N/A", List.of()));

        StudioSearchRequest request = new StudioSearchRequest();
        request.setQuery("studio");

        List<StudioSearchResult> results = searchService.searchStudios(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("studio-1");
        verify(searchAnalyticsService).recordSearch(SearchEntityType.STUDIO, "studio", 77, 1);
    }
}

package com.studioos.server.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.studioos.server.search.dto.AutocompleteSuggestion;
import com.studioos.server.search.dto.BeatSearchRequest;
import com.studioos.server.search.dto.BeatSearchResult;
import com.studioos.server.search.dto.StudioSearchRequest;
import com.studioos.server.search.dto.StudioSearchResult;
import com.studioos.server.search.dto.SearchPageResponse;
import com.studioos.server.shared.enums.SearchEntityType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchFacadeService searchFacadeService;

    @InjectMocks
    private SearchController searchController;

    @Test
    void searchBeatsDelegatesToFacade() {
        BeatSearchRequest request = new BeatSearchRequest();
        BeatSearchResult result = BeatSearchResult.builder().id("beat-1").title("Night Ride").build();
        SearchPageResponse<BeatSearchResult> response = SearchPageResponse.<BeatSearchResult>builder()
                .results(List.of(result)).page(0).size(20).total(1).build();
        when(searchFacadeService.searchBeats(request)).thenReturn(response);

        assertThat(searchController.searchBeats(request).getResults()).containsExactly(result);
    }

    @Test
    void searchStudiosDelegatesToFacade() {
        StudioSearchRequest request = new StudioSearchRequest();
        StudioSearchResult result = StudioSearchResult.builder().id("studio-1").studioName("Studio One").build();
        SearchPageResponse<StudioSearchResult> response = SearchPageResponse.<StudioSearchResult>builder()
                .results(List.of(result)).page(0).size(20).total(1).build();
        when(searchFacadeService.searchStudios(request)).thenReturn(response);

        assertThat(searchController.searchStudios(request).getResults()).containsExactly(result);
    }

    @Test
    void suggestDelegatesToFacade() {
        AutocompleteSuggestion suggestion = new AutocompleteSuggestion(
                "beat-1", "Night Ride", SearchEntityType.BEAT);
        when(searchFacadeService.suggest("night")).thenReturn(List.of(suggestion));

        assertThat(searchController.suggest("night")).containsExactly(suggestion);
    }
}

package com.studioos.server.search;

import com.studioos.server.search.dto.AutocompleteSuggestion;
import com.studioos.server.search.dto.AdvertisementSearchResult;
import com.studioos.server.search.dto.BeatSearchRequest;
import com.studioos.server.search.dto.BeatSearchResult;
import com.studioos.server.search.dto.ProducerSearchResult;
import com.studioos.server.search.dto.RecentSearchResponse;
import com.studioos.server.search.dto.SearchReindexResponse;
import com.studioos.server.search.dto.SearchRequest;
import com.studioos.server.search.dto.SearchResponseDto;
import com.studioos.server.search.dto.TrendingResponse;
import com.studioos.server.search.dto.StudioSearchRequest;
import com.studioos.server.search.dto.StudioSearchResult;
import com.studioos.server.search.index.SearchIndexService;
import com.studioos.server.search.service.AdvertisementSearchService;
import com.studioos.server.search.service.GlobalSearchService;
import com.studioos.server.search.service.ProducerSearchService;
import com.studioos.server.search.trending.TrendingSearchService;
import com.studioos.server.search.cache.SearchCacheService;
import com.studioos.server.shared.enums.SearchEntityType;
import com.studioos.server.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchFacadeService {

    private final OpenSearchBeatSearchService beatSearchService;
    private final OpenSearchStudioSearchService studioSearchService;
    private final GlobalSearchService globalSearchService;
    private final ProducerSearchService producerSearchService;
    private final AdvertisementSearchService advertisementSearchService;
    private final TrendingSearchService trendingSearchService;
    private final SearchCacheService searchCacheService;
    private final SearchIndexService searchIndexService;

    public List<BeatSearchResult> searchBeats(BeatSearchRequest request) {
        return beatSearchService.searchBeats(request);
    }

    public List<StudioSearchResult> searchStudios(StudioSearchRequest request) {
        return studioSearchService.searchStudios(request);
    }

    public List<AutocompleteSuggestion> suggest(String q) {
        return beatSearchService.suggest(q);
    }

    public SearchResponseDto search(SearchRequest request) {
        return globalSearchService.search(request);
    }

    public List<ProducerSearchResult> searchProducers(String query, int page, int size) {
        return producerSearchService.search(query, page, size);
    }

    public List<AdvertisementSearchResult> searchAdvertisements(String query, int page, int size) {
        return advertisementSearchService.search(query, page, size);
    }

    public List<TrendingResponse> trending(SearchEntityType entityType, int limit) {
        return trendingSearchService.trending(entityType, limit);
    }

    public List<RecentSearchResponse> recent(User user) {
        return searchCacheService.getRecentSearches(user != null ? user.getId() : null);
    }

    public void clearRecent(User user) {
        searchCacheService.clearRecentSearches(user != null ? user.getId() : null);
    }

    public SearchReindexResponse reindexAll() {
        searchIndexService.reindexAll();
        return SearchReindexResponse.builder().success(true).message("Search reindex started").build();
    }
}

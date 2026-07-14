package com.studioos.server.search.service;

import com.studioos.server.search.OpenSearchBeatSearchService;
import com.studioos.server.search.OpenSearchStudioSearchService;
import com.studioos.server.search.cache.SearchCacheService;
import com.studioos.server.search.dto.AdvertisementSearchResult;
import com.studioos.server.search.dto.BeatSearchRequest;
import com.studioos.server.search.dto.BeatSearchResult;
import com.studioos.server.search.dto.ProducerSearchResult;
import com.studioos.server.search.dto.SearchRequest;
import com.studioos.server.search.dto.SearchResponseDto;
import com.studioos.server.search.dto.SearchResultItem;
import com.studioos.server.search.dto.StudioSearchRequest;
import com.studioos.server.search.dto.StudioSearchResult;
import com.studioos.server.shared.enums.SearchEntityType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final OpenSearchBeatSearchService beatSearchService;
    private final OpenSearchStudioSearchService studioSearchService;
    private final ProducerSearchService producerSearchService;
    private final AdvertisementSearchService advertisementSearchService;
    private final SearchCacheService searchCacheService;

    public SearchResponseDto search(SearchRequest request) {
        String cacheKey = cacheKey(request);
        SearchResponseDto cached = searchCacheService.readSearchResult(cacheKey, SearchResponseDto.class);
        if (cached != null) {
            return cached;
        }

        List<SearchResultItem> results = new ArrayList<>();

        if (request.getEntityType() == null || request.getEntityType() == SearchEntityType.BEAT) {
            BeatSearchRequest beatRequest = new BeatSearchRequest();
            beatRequest.setQuery(request.getQuery());
            beatRequest.setGenre(request.getGenre());
            beatRequest.setPage(request.getPage());
            beatRequest.setSize(request.getSize());
            for (BeatSearchResult result : beatSearchService.searchBeats(beatRequest)) {
                results.add(SearchResultItem.builder()
                        .entityType(SearchEntityType.BEAT)
                        .id(result.getId())
                        .title(result.getTitle())
                        .subtitle(result.getGenre())
                        .score(result.getRankScore() != null ? result.getRankScore() : result.getScore())
                        .build());
            }
        }

        if (request.getEntityType() == null || request.getEntityType() == SearchEntityType.STUDIO) {
            StudioSearchRequest studioRequest = new StudioSearchRequest();
            studioRequest.setQuery(request.getQuery());
            studioRequest.setLocation(request.getLocation());
            studioRequest.setPage(request.getPage());
            studioRequest.setSize(request.getSize());
            for (StudioSearchResult result : studioSearchService.searchStudios(studioRequest)) {
                results.add(SearchResultItem.builder()
                        .entityType(SearchEntityType.STUDIO)
                        .id(result.getId())
                        .title(result.getStudioName())
                        .subtitle(result.getLocation())
                        .score(result.getScore())
                        .build());
            }
        }

        if (request.getEntityType() == null || request.getEntityType() == SearchEntityType.PRODUCER) {
            for (ProducerSearchResult result : producerSearchService.search(request.getQuery(), request.getPage(), request.getSize())) {
                results.add(SearchResultItem.builder()
                        .entityType(SearchEntityType.PRODUCER)
                        .id(String.valueOf(result.getId()))
                        .title(result.getName())
                        .subtitle(result.getGenre())
                        .score(result.getScore())
                        .build());
            }
        }

        if (request.getEntityType() == null || request.getEntityType() == SearchEntityType.ADVERTISEMENT) {
            for (AdvertisementSearchResult result : advertisementSearchService.search(request.getQuery(), request.getPage(), request.getSize())) {
                results.add(SearchResultItem.builder()
                        .entityType(SearchEntityType.ADVERTISEMENT)
                        .id(result.getId())
                        .title(result.getHeadline())
                        .subtitle(result.getType() != null ? result.getType().name() : null)
                        .score(result.getScore())
                        .build());
            }
        }

        results.sort(Comparator.comparingDouble((SearchResultItem item) ->
                item.getScore() == null ? 0.0 : item.getScore()).reversed());
        SearchResponseDto response = SearchResponseDto.builder()
                .results(results)
                .page(request.getPage())
                .size(request.getSize())
                .total(results.size())
                .build();
        searchCacheService.cacheSearchResult(cacheKey, response, Duration.ofMinutes(5));
        return response;
    }

    private String cacheKey(SearchRequest request) {
        return "global:%s:%s:%s:%s:%s:%d:%d".formatted(
                request.getEntityType(),
                request.getQuery(),
                request.getLocation(),
                request.getGenre(),
                request.getMinPrice(),
                request.getPage(),
                request.getSize());
    }
}

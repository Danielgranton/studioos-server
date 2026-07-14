package com.studioos.server.search.trending;

import com.studioos.server.search.analytics.SearchEventRepository;
import com.studioos.server.search.cache.SearchCacheService;
import com.studioos.server.search.dto.TrendingResponse;
import com.studioos.server.shared.enums.SearchEntityType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrendingSearchService {

    private final SearchCacheService searchCacheService;
    private final SearchEventRepository searchEventRepository;

    public List<TrendingResponse> trending(SearchEntityType entityType, int limit) {
        List<TrendingResponse> cached = searchCacheService.getTrending(entityType, limit);
        if (!cached.isEmpty()) {
            return cached;
        }

        return searchEventRepository.findTopSearches(entityType, LocalDateTime.now().minusDays(7)).stream()
                .map(item -> TrendingResponse.builder()
                        .entityType(entityType)
                        .query(item.getQuery())
                        .count(item.getSearchCount())
                        .build())
                .toList();
    }
}

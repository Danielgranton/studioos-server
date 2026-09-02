package com.studioos.server.search.analytics;

import org.springframework.stereotype.Service;
import com.studioos.server.search.cache.SearchCacheService;
import com.studioos.server.shared.enums.SearchEntityType;
import com.studioos.server.user.PrivacySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAnalyticsService {

    private final SearchEventRepository searchEventRepository;
    private final SearchCacheService searchCacheService;
    private final PrivacySettingsService privacySettingsService;

    public void recordSearch(SearchEntityType entityType, String query, Integer userId, int resultCount) {
        try {
            if (userId != null && !privacySettingsService.isPersonalizedRecommendationsEnabled(userId)) {
                // Keep anonymous aggregate trends, but do not retain user-specific search history.
                searchCacheService.incrementTrending(entityType, query);
                return;
            }
            SearchEvent event = SearchEvent.builder()
                    .entityType(entityType)
                    .query(query)
                    .userId(userId)
                    .resultCount(resultCount)
                    .build();
            searchEventRepository.save(event);
            searchCacheService.incrementTrending(entityType, query);
            searchCacheService.recordRecentSearch(userId, entityType, query);
        } catch (Exception e) {
            // Analytics failure must never block a search response — fail-open,
            // same posture as indexing/notification failures elsewhere in this codebase.
            log.error("Failed to record search event: {}", e.getMessage());
        }
    }
}

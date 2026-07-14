package com.studioos.server.search.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.server.search.dto.RecentSearchResponse;
import com.studioos.server.search.dto.TrendingResponse;
import com.studioos.server.shared.enums.SearchEntityType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchCacheService {

    private static final String RECENT_PREFIX = "search:recent:";
    private static final String TRENDING_PREFIX = "search:trending:";
    private static final String RESULT_PREFIX = "search:result:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void cacheSearchResult(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(RESULT_PREFIX + key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ignored) {
        }
    }

    public <T> T readSearchResult(String key, Class<T> type) {
        String raw = redisTemplate.opsForValue().get(RESULT_PREFIX + key);
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (Exception e) {
            return null;
        }
    }

    public void recordRecentSearch(Integer userId, SearchEntityType entityType, String query) {
        if (userId == null || query == null || query.isBlank()) {
            return;
        }
        String key = RECENT_PREFIX + userId;
        String entry = entityType + "|" + query;
        redisTemplate.opsForList().remove(key, 0, entry);
        redisTemplate.opsForList().leftPush(key, entry);
        redisTemplate.opsForList().trim(key, 0, 9);
    }

    public List<RecentSearchResponse> getRecentSearches(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        List<String> raw = redisTemplate.opsForList().range(RECENT_PREFIX + userId, 0, 9);
        if (raw == null) {
            return List.of();
        }
        List<RecentSearchResponse> result = new ArrayList<>();
        for (String item : raw) {
            String[] parts = item.split("\\|", 2);
            SearchEntityType type = parts.length > 0 ? SearchEntityType.valueOf(parts[0]) : SearchEntityType.GLOBAL;
            String query = parts.length > 1 ? parts[1] : "";
            result.add(RecentSearchResponse.builder().entityType(type).query(query).build());
        }
        return result;
    }

    public void clearRecentSearches(Integer userId) {
        if (userId != null) {
            redisTemplate.delete(RECENT_PREFIX + userId);
        }
    }

    public void incrementTrending(SearchEntityType entityType, String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        redisTemplate.opsForZSet().incrementScore(TRENDING_PREFIX + entityType.name(), query.toLowerCase(), 1.0);
    }

    public List<TrendingResponse> getTrending(SearchEntityType entityType, int limit) {
        Set<String> raw = redisTemplate.opsForZSet().reverseRange(TRENDING_PREFIX + entityType.name(), 0, limit - 1);
        if (raw == null) {
            return List.of();
        }
        List<TrendingResponse> result = new ArrayList<>();
        for (String query : raw) {
            Double score = redisTemplate.opsForZSet().score(TRENDING_PREFIX + entityType.name(), query);
            result.add(TrendingResponse.builder()
                    .entityType(entityType)
                    .query(query)
                    .count(score == null ? 0L : score.longValue())
                    .build());
        }
        return result;
    }
}

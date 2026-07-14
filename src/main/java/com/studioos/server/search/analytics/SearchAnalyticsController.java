package com.studioos.server.search.analytics;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.studioos.server.shared.enums.SearchEntityType;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/search/analytics")
@RequiredArgsConstructor
public class SearchAnalyticsController {

    private final SearchEventRepository searchEventRepository;

    @GetMapping("/top")
    public List<SearchEventRepository.TopSearchProjection> topSearches(
            @RequestParam SearchEntityType entityType,
            @RequestParam(defaultValue = "7") int days) {
        return searchEventRepository.findTopSearches(entityType, LocalDateTime.now().minusDays(days));
    }

    @GetMapping("/no-results")
    public List<SearchEventRepository.TopSearchProjection> noResultSearches(
            @RequestParam SearchEntityType entityType,
            @RequestParam(defaultValue = "7") int days) {
        return searchEventRepository.findNoResultSearches(entityType, LocalDateTime.now().minusDays(days));
    }
}
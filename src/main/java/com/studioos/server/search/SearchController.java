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
import com.studioos.server.shared.enums.SearchEntityType;
import com.studioos.server.user.User;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchFacadeService searchFacadeService;

    @GetMapping("/beats")
    public List<BeatSearchResult> searchBeats(@ModelAttribute BeatSearchRequest request) {
        return searchFacadeService.searchBeats(request);
    }

    @GetMapping("/studios")
    public List<StudioSearchResult> searchStudios(@ModelAttribute StudioSearchRequest request) {
        return searchFacadeService.searchStudios(request);
    }

    @GetMapping("/suggestions")
    public List<AutocompleteSuggestion> suggest(@RequestParam String q) {
        return searchFacadeService.suggest(q);
    }

    @GetMapping
    public SearchResponseDto search(@ModelAttribute SearchRequest request) {
        return searchFacadeService.search(request);
    }

    @GetMapping("/advertisements")
    public List<AdvertisementSearchResult> searchAdvertisements(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return searchFacadeService.searchAdvertisements(q, page, size);
    }

    @GetMapping("/producers")
    public List<ProducerSearchResult> searchProducers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return searchFacadeService.searchProducers(q, page, size);
    }

    @GetMapping("/trending")
    public List<TrendingResponse> trending(
            @RequestParam SearchEntityType entityType,
            @RequestParam(defaultValue = "10") int limit) {
        return searchFacadeService.trending(entityType, limit);
    }

    @GetMapping("/recent")
    public List<RecentSearchResponse> recent(@AuthenticationPrincipal User user) {
        return searchFacadeService.recent(user);
    }

    @DeleteMapping("/recent")
    public void clearRecent(@AuthenticationPrincipal User user) {
        searchFacadeService.clearRecent(user);
    }

    @PostMapping("/reindex")
    public SearchReindexResponse reindex() {
        return searchFacadeService.reindexAll();
    }
}

package com.studioos.server.search;

import com.studioos.server.search.dto.BeatSearchRequest;
import com.studioos.server.search.dto.BeatSearchResult;
import com.studioos.server.search.dto.SearchPageResponse;
import java.util.List;

public interface SearchService {
    SearchPageResponse<BeatSearchResult> searchBeats(BeatSearchRequest request);
}

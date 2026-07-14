package com.studioos.server.search;

import com.studioos.server.search.dto.BeatSearchRequest;
import com.studioos.server.search.dto.BeatSearchResult;
import java.util.List;

public interface SearchService {
    List<BeatSearchResult> searchBeats(BeatSearchRequest request);
}
package com.studioos.server.search;

import com.studioos.server.search.dto.SearchReindexResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/search")
@RequiredArgsConstructor
public class AdminSearchController {

    private final SearchFacadeService searchFacadeService;

    @PostMapping("/reindex")
    public SearchReindexResponse reindex() {
        return searchFacadeService.reindexAll();
    }
}

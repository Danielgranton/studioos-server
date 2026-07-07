package com.studioos.server.beatmarketplace;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.beatmarketplace.dto.BeatSearchRequest;
import com.studioos.server.beatmarketplace.dto.BeatSummaryResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/beats")
@RequiredArgsConstructor
public class BeatBrowseController {

    private final BeatBrowseService beatBrowseService;

    @GetMapping
    public Page<BeatSummaryResponse> browse(@ModelAttribute BeatSearchRequest request) {
        return beatBrowseService.search(request);
    }

    @GetMapping("/search")
    public Page<BeatSummaryResponse> search(@ModelAttribute BeatSearchRequest request) {
        return beatBrowseService.search(request);
    }
}
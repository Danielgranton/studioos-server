package com.studioos.server.help;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.help.dto.HelpArticleResponse;
import com.studioos.server.help.dto.HelpArticleSummaryResponse;
import com.studioos.server.help.dto.HelpCategoryResponse;
import com.studioos.server.help.dto.HelpFeedbackRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/help")
@RequiredArgsConstructor
public class HelpArticleController {

    private final HelpArticleService helpArticleService;

    @GetMapping("/categories")
    public List<HelpCategoryResponse> categories() {
        return helpArticleService.getCategories();
    }

    @GetMapping("/articles")
    public Page<HelpArticleSummaryResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) HelpAudience audience,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return helpArticleService.search(query, category, audience, page, size);
    }

    @GetMapping("/articles/popular")
    public Page<HelpArticleSummaryResponse> popular(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return helpArticleService.getPopular(page, size);
    }

    @GetMapping("/articles/{slug}")
    public HelpArticleResponse article(@PathVariable String slug) {
        return helpArticleService.getBySlug(slug);
    }

    @PostMapping("/articles/{slug}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void feedback(@PathVariable String slug, @RequestBody HelpFeedbackRequest request) {
        helpArticleService.recordFeedback(slug, request.helpful());
    }
}

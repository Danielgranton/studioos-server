package com.studioos.server.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.help.HelpArticleService;
import com.studioos.server.help.dto.HelpArticleAdminResponse;
import com.studioos.server.help.dto.HelpArticleUpsertRequest;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/help/articles")
@RequiredArgsConstructor
@Validated
public class AdminHelpController {

    private final HelpArticleService helpArticleService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HelpArticleAdminResponse>>> articles(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(helpArticleService.getAdminArticles(pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HelpArticleAdminResponse>> create(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody HelpArticleUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Help article created", helpArticleService.createArticle(admin, request)));
    }

    @PutMapping("/{articleId}")
    public ResponseEntity<ApiResponse<HelpArticleAdminResponse>> update(
            @PathVariable String articleId,
            @Valid @RequestBody HelpArticleUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Help article updated", helpArticleService.updateArticle(articleId, request)));
    }

    @PostMapping("/{articleId}/publish")
    public ResponseEntity<ApiResponse<HelpArticleAdminResponse>> publish(@PathVariable String articleId) {
        return ResponseEntity.ok(ApiResponse.success("Help article published", helpArticleService.publishArticle(articleId)));
    }

    @PostMapping("/{articleId}/archive")
    public ResponseEntity<ApiResponse<HelpArticleAdminResponse>> archive(@PathVariable String articleId) {
        return ResponseEntity.ok(ApiResponse.success("Help article archived", helpArticleService.archiveArticle(articleId)));
    }
}

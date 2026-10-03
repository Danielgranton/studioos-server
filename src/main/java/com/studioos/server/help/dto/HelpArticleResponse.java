package com.studioos.server.help.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.studioos.server.help.HelpAudience;

import lombok.Builder;

@Builder
public record HelpArticleResponse(
        String id,
        String slug,
        String title,
        String excerpt,
        String content,
        String categorySlug,
        String categoryName,
        HelpAudience audience,
        Integer readTimeMinutes,
        Boolean featured,
        Long viewCount,
        LocalDateTime publishedAt,
        Set<String> tags,
        List<HelpArticleSummaryResponse> relatedArticles) {
}

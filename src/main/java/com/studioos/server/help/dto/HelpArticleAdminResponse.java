package com.studioos.server.help.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.studioos.server.help.HelpArticleStatus;
import com.studioos.server.help.HelpAudience;

public record HelpArticleAdminResponse(
        String id,
        String slug,
        String title,
        String excerpt,
        String content,
        String categorySlug,
        String categoryName,
        HelpArticleStatus status,
        HelpAudience audience,
        Boolean featured,
        Integer displayOrder,
        Integer readTimeMinutes,
        Long viewCount,
        Long helpfulCount,
        Long notHelpfulCount,
        LocalDateTime publishedAt,
        Set<String> tags,
        LocalDateTime updatedAt) {
}

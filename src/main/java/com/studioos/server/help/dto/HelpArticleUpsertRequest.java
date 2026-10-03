package com.studioos.server.help.dto;

import java.util.List;

import com.studioos.server.help.HelpAudience;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HelpArticleUpsertRequest(
        @NotBlank @Size(max = 160) String slug,
        @NotBlank @Size(max = 180) String title,
        @NotBlank String excerpt,
        @NotBlank String content,
        @NotBlank String categorySlug,
        @NotNull HelpAudience audience,
        Boolean featured,
        @Min(0) Integer displayOrder,
        @Min(1) Integer readTimeMinutes,
        List<@NotBlank @Size(max = 60) String> tags) {
}

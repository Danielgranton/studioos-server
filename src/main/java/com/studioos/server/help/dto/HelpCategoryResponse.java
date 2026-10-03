package com.studioos.server.help.dto;

import lombok.Builder;

@Builder
public record HelpCategoryResponse(
        String id,
        String slug,
        String name,
        String description,
        Integer displayOrder) {
}

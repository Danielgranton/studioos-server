package com.studioos.server.servicecatalog.dto;

public record ServiceCatalogResponse(
        String id,
        String slug,
        String name,
        String category,
        String description,
        boolean artistAllowed,
        boolean studioAllowed) {
}

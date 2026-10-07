package com.studioos.server.servicecatalog.dto;

public record ServiceProviderResponse(
        String providerType,
        String providerId,
        String providerName,
        String location,
        String profileImage,
        String serviceName,
        String description,
        Integer price,
        String currency,
        boolean verified,
        String listingId,
        String studioId,
        String catalogServiceId,
        String priceType) {
}

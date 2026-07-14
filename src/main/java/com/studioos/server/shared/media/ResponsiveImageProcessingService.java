package com.studioos.server.shared.media;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.studioos.server.shared.exceptions.StudioosException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResponsiveImageProcessingService {

    private static final int DEFAULT_QUALITY = 80;

    private final MediaProcessingClient mediaProcessingClient;

    public ResponsiveImageAsset process(String assetReference, String objectKeyPrefix) {
        if (assetReference == null || assetReference.isBlank()) {
            return null;
        }

        MediaResponsiveImageResult result = mediaProcessingClient.processResponsiveImage(
                assetReference,
                objectKeyPrefix,
                DEFAULT_QUALITY);

        if (result == null || result.getOriginalUrl() == null || result.getOriginalUrl().isBlank()) {
            throw StudioosException.badRequest("Responsive image processing did not return an image URL");
        }

        Map<Integer, String> variantsBySize = new HashMap<>();
        if (result.getVariants() != null) {
            result.getVariants().forEach(variant -> {
                if (variant != null && variant.getSize() != null && variant.getUrl() != null && !variant.getUrl().isBlank()) {
                    variantsBySize.put(variant.getSize(), variant.getUrl());
                }
            });
        }

        return ResponsiveImageAsset.builder()
                .originalUrl(result.getOriginalUrl())
                .largeUrl(variantsBySize.get(1024))
                .mediumUrl(variantsBySize.get(512))
                .thumbnailUrl(variantsBySize.get(128))
                .build();
    }
}

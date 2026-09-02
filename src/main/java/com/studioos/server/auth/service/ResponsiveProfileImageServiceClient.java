package com.studioos.server.auth.service;

import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.studioos.server.shared.media.ResponsiveImageAsset;
import com.studioos.server.shared.media.ResponsiveImageProcessingService;
import com.studioos.server.shared.media.MediaProcessingClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResponsiveProfileImageServiceClient implements ProfileImageServiceClient {

    private final ResponsiveImageProcessingService responsiveImageProcessingService;
    private final MediaProcessingClient mediaProcessingClient;

    @Override
    public ResponsiveImageAsset processProfileImage(String profileImageReference, String storagePrefix) {
        return responsiveImageProcessingService.process(profileImageReference, storagePrefix);
    }

    @Override
    public ResponsiveImageAsset processUploadedProfileImage(
            InputStream content, long contentLength, String filename, String contentType,
            String storagePrefix, String ownerId) {
        String assetReference = mediaProcessingClient.uploadMedia(
                content, contentLength, filename, contentType, ownerId);
        return responsiveImageProcessingService.process(assetReference, storagePrefix);
    }
}

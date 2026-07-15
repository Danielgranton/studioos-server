package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;

import com.studioos.server.shared.media.ResponsiveImageAsset;
import com.studioos.server.shared.media.ResponsiveImageProcessingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResponsiveProfileImageServiceClient implements ProfileImageServiceClient {

    private final ResponsiveImageProcessingService responsiveImageProcessingService;

    @Override
    public ResponsiveImageAsset processProfileImage(String profileImageReference, String storagePrefix) {
        return responsiveImageProcessingService.process(profileImageReference, storagePrefix);
    }
}

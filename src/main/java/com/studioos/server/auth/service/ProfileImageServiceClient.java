package com.studioos.server.auth.service;

import java.io.InputStream;

import com.studioos.server.shared.media.ResponsiveImageAsset;

public interface ProfileImageServiceClient {
    ResponsiveImageAsset processProfileImage(String profileImageReference, String storagePrefix);

    ResponsiveImageAsset processUploadedProfileImage(
            InputStream content, long contentLength, String filename, String contentType,
            String storagePrefix, String ownerId);
}

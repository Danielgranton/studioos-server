package com.studioos.server.auth.service;

import com.studioos.server.shared.media.ResponsiveImageAsset;

public interface ProfileImageServiceClient {
    ResponsiveImageAsset processProfileImage(String profileImageReference, String storagePrefix);
}

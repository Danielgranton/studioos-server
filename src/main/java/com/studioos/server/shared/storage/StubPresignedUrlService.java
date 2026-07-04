package com.studioos.server.shared.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!s3-enabled")
public class StubPresignedUrlService implements PresignedUrlService {

    @Override
    public String generateUploadUrl(String bucket, String objectKey, String contentType, int expirySeconds) {
        return "https://stub-upload.local/" + bucket + "/" + objectKey + "?expires=" + expirySeconds;
    }

    @Override
    public String generateDownloadUrl(String bucket, String objectKey, int expirySeconds) {
        return "https://stub-download.local/" + bucket + "/" + objectKey + "?expires=" + expirySeconds;
    }

    @Override
    public boolean objectExists(String bucket, String objectKey) {
        return true;
    }
}
package com.studioos.server.shared.storage;

import java.time.Instant;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "storage.s3.enabled", havingValue = "false", matchIfMissing = true)
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
    public Optional<StorageObjectMetadata> objectMetadata(String bucket, String objectKey) {
        return Optional.of(new StorageObjectMetadata(1L, null, "stub", Instant.now()));
    }
}

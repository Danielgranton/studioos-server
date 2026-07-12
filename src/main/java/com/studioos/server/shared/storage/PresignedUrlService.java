package com.studioos.server.shared.storage;

import java.util.Optional;

public interface PresignedUrlService {
    String generateUploadUrl(String bucket, String objectKey, String contentType, int expirySeconds);
    String generateDownloadUrl(String bucket, String objectKey, int expirySeconds);
    Optional<StorageObjectMetadata> objectMetadata(String bucket, String objectKey);

    default boolean objectExists(String bucket, String objectKey) {
        return objectMetadata(bucket, objectKey).isPresent();
    }
}

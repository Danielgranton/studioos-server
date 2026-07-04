package com.studioos.server.shared.storage;

public interface PresignedUrlService {
    String generateUploadUrl(String bucket, String objectKey, String contentType, int expirySeconds);
    String generateDownloadUrl(String bucket, String objectKey, int expirySeconds);
    boolean objectExists(String bucket, String objectKey);
}
package com.studioos.server.shared.storage;

import java.time.Duration;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.s3.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AwsPresignedUrlService implements PresignedUrlService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Override
    public String generateUploadUrl(String bucket, String objectKey, String contentType, int expirySeconds) {
        PutObjectRequest.Builder putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey);

        if (contentType != null && !contentType.isBlank()) {
            putRequest.contentType(contentType);
        }

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .putObjectRequest(putRequest.build())
                .build();

        return s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    }

    @Override
    public String generateDownloadUrl(String bucket, String objectKey, int expirySeconds) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    @Override
    public Optional<StorageObjectMetadata> objectMetadata(String bucket, String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            return Optional.of(new StorageObjectMetadata(
                    response.contentLength(),
                    response.contentType(),
                    response.eTag(),
                    response.lastModified()));
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                log.warn("S3 headObject failed for s3://{}/{}: {}", bucket, objectKey, e.getMessage());
            }
            return Optional.empty();
        } catch (SdkException e) {
            log.warn("S3 headObject failed for s3://{}/{}: {}", bucket, objectKey, e.getMessage());
            return Optional.empty();
        }
    }
}

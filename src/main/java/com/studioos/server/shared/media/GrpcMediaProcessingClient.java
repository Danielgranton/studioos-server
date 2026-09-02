package com.studioos.server.shared.media;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Callable;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.studioos.server.shared.enums.MediaJobStatus;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import media.Media;
import media.MediaServiceGrpc;

@Slf4j
@Service
@Profile("grpc-enabled")
public class GrpcMediaProcessingClient implements MediaProcessingClient {

    private final ManagedChannel channel;
    private final MediaServiceGrpc.MediaServiceBlockingStub blockingStub;
    private final long rpcTimeoutMs;
    private final long uploadTimeoutMs;
    private final int retryMaxAttempts;
    private final long retryInitialBackoffMs;
    private final long retryMaxBackoffMs;

    public GrpcMediaProcessingClient(
            @Value("${media.service.host:127.0.0.1}") String host,
            @Value("${media.service.port:50051}") int port,
            @Value("${media.service.tls.enabled:false}") boolean tlsEnabled,
            @Value("${media.service.tls.trust-certificates:}") String trustCertificates,
            @Value("${media.service.tls.client-certificate:}") String clientCertificate,
            @Value("${media.service.tls.client-key:}") String clientKey,
            @Value("${media.service.rpc-timeout-ms:30000}") long rpcTimeoutMs,
            @Value("${media.service.upload-timeout-ms:60000}") long uploadTimeoutMs,
            @Value("${media.service.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${media.service.retry.initial-backoff-ms:100}") long retryInitialBackoffMs,
            @Value("${media.service.retry.max-backoff-ms:1000}") long retryMaxBackoffMs) {
        if (rpcTimeoutMs <= 0 || uploadTimeoutMs <= 0
                || retryMaxAttempts < 1 || retryInitialBackoffMs < 0
                || retryMaxBackoffMs < retryInitialBackoffMs) {
            throw new IllegalStateException("Invalid media gRPC timeout or retry configuration");
        }
        this.rpcTimeoutMs = rpcTimeoutMs;
        this.uploadTimeoutMs = uploadTimeoutMs;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryInitialBackoffMs = retryInitialBackoffMs;
        this.retryMaxBackoffMs = retryMaxBackoffMs;
        if (tlsEnabled) {
            if (trustCertificates.isBlank() || clientCertificate.isBlank() || clientKey.isBlank()) {
                throw new IllegalStateException("Media TLS requires CA, client certificate, and client key files");
            }
            try {
                var sslContext = GrpcSslContexts.forClient()
                        .trustManager(Path.of(trustCertificates).toFile())
                        .keyManager(Path.of(clientCertificate).toFile(), Path.of(clientKey).toFile())
                        .build();
                this.channel = NettyChannelBuilder.forAddress(host, port)
                        .sslContext(sslContext)
                        .build();
            } catch (Exception e) {
                throw new IllegalStateException("Could not configure media mTLS", e);
            }
        } else {
            this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        }
        this.blockingStub = MediaServiceGrpc.newBlockingStub(channel);
        log.info("Configured StudioOS Media gRPC client for {}:{}", host, port);
    }

    @Override
    public boolean health() {
        Media.HealthResponse response = withRetry(
                () -> timedBlockingStub().health(Media.HealthRequest.newBuilder().build()), "health");
        return response != null && response.getStatus() != null
                && response.getStatus().equalsIgnoreCase("SERVING");
    }

    @Override
    public String submitJob(String assetReference, String operation, String parametersJson) {
        Media.MediaJobRequest request = Media.MediaJobRequest.newBuilder()
                .setAssetReference(assetReference)
                .setOperation(operation)
                .setParametersJson(parametersJson == null || parametersJson.isBlank() ? "{}" : parametersJson)
                .build();

        Media.MediaJobResponse response = timedBlockingStub().submitMediaJob(request);
        log.info("Submitted media job {} op={} asset={} status={}",
                response.getJobId(), response.getOperation(), response.getAssetReference(), response.getStatus());
        return response.getJobId();
    }

    @Override
    public MediaJobResult getJobStatus(String externalJobId) {
        Media.MediaJobLookupRequest request = Media.MediaJobLookupRequest.newBuilder()
                .setJobId(externalJobId)
                .build();

        Media.MediaJobResponse response = withRetry(
                () -> timedBlockingStub().getMediaJob(request), "get job status");
        return MediaJobResult.builder()
                .jobId(response.getJobId())
                .status(toMediaJobStatus(response.getStatus()))
                .assetReference(blankToNull(response.getAssetReference()))
                .operation(blankToNull(response.getOperation()))
                .parametersJson(blankToNull(response.getParametersJson()))
                .resultReference(blankToNull(response.getResultReference()))
                .errorMessage(blankToNull(response.getErrorMessage()))
                .createdAtUnixMs(response.getCreatedAtUnixMs())
                .updatedAtUnixMs(response.getUpdatedAtUnixMs())
                .build();
    }

    @Override
    public MediaResponsiveImageResult processResponsiveImage(String assetReference, String objectKeyPrefix, int quality) {
        Media.ResponsiveImageRequest request = Media.ResponsiveImageRequest.newBuilder()
                .setImagePath(assetReference)
                .setObjectKeyPrefix(objectKeyPrefix == null ? "" : objectKeyPrefix)
                .setQuality(quality)
                .build();

        Media.ResponsiveImageResponse response = withRetry(
                () -> timedBlockingStub().processResponsiveImage(request), "responsive image processing");

        return mapResponsiveImage(response);
    }

    @Override
    public String uploadMedia(InputStream content, long contentLength, String filename, String contentType, String ownerId) {
        if (contentLength < 1 || contentLength > 5L * 1024L * 1024L) {
            throw new IllegalArgumentException("Media upload must be between 1 byte and 5 MB");
        }
        AtomicReference<Media.MediaJobResponse> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        StreamObserver<Media.MediaUploadChunk> requestObserver = MediaServiceGrpc.newStub(channel)
                .withDeadlineAfter(uploadTimeoutMs, TimeUnit.MILLISECONDS)
                .uploadMedia(new StreamObserver<>() {
                    @Override public void onNext(Media.MediaJobResponse value) { result.set(value); }
                    @Override public void onError(Throwable throwable) { failure.set(throwable); completed.countDown(); }
                    @Override public void onCompleted() { completed.countDown(); }
                });

        requestObserver.onNext(Media.MediaUploadChunk.newBuilder()
                .setMetadata(Media.MediaUploadMetadata.newBuilder()
                        .setFilename(filename == null ? "profile-image" : filename)
                        .setContentType(contentType == null ? "image/jpeg" : contentType)
                        .setMediaType("image")
                        .setOwnerId(ownerId == null ? "" : ownerId)
                        .setOperation("profile-image-upload")
                        .build())
                .build());

        final byte[] buffer = new byte[64 * 1024];
        long uploaded = 0;
        try {
            int read;
            while ((read = content.read(buffer)) != -1) {
                uploaded += read;
                if (uploaded > contentLength || uploaded > 5L * 1024L * 1024L) {
                    requestObserver.onError(new IllegalArgumentException("Media upload exceeds the allowed size"));
                    throw new IllegalArgumentException("Media upload exceeds the allowed size");
                }
                requestObserver.onNext(Media.MediaUploadChunk.newBuilder()
                        .setData(com.google.protobuf.ByteString.copyFrom(buffer, 0, read))
                        .build());
            }
        } catch (IOException e) {
            requestObserver.onError(e);
            throw new IllegalStateException("Could not read media upload", e);
        }
        if (uploaded != contentLength) {
            requestObserver.onError(new IllegalArgumentException("Media upload length did not match its metadata"));
            throw new IllegalArgumentException("Media upload length did not match its metadata");
        }
        requestObserver.onCompleted();

        try {
            if (!completed.await(uploadTimeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Media upload timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Media upload interrupted", e);
        }
        if (failure.get() != null) throw new IllegalStateException("Media upload failed", failure.get());
        if (result.get() == null || result.get().getAssetReference().isBlank()) {
            throw new IllegalStateException("Media service returned no uploaded asset reference");
        }
        return result.get().getAssetReference();
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private MediaJobStatus toMediaJobStatus(String status) {
        if (status == null) {
            return MediaJobStatus.PENDING;
        }

        return switch (status.toUpperCase(Locale.ROOT)) {
            case "QUEUED" -> MediaJobStatus.QUEUED;
            case "RUNNING", "PROCESSING" -> MediaJobStatus.RUNNING;
            case "SUCCESS", "SUCCEEDED", "COMPLETED", "DONE" -> MediaJobStatus.SUCCESS;
            case "FAILED", "ERROR" -> MediaJobStatus.FAILED;
            case "PENDING" -> MediaJobStatus.PENDING;
            default -> MediaJobStatus.QUEUED;
        };
    }

    private MediaServiceGrpc.MediaServiceBlockingStub timedBlockingStub() {
        return blockingStub.withDeadlineAfter(rpcTimeoutMs, TimeUnit.MILLISECONDS);
    }

    private <T> T withRetry(Callable<T> operation, String operationName) {
        long backoffMs = retryInitialBackoffMs;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                return operation.call();
            } catch (Exception error) {
                if (!isRetryable(error) || attempt == retryMaxAttempts) {
                    throw new IllegalStateException("Media " + operationName + " failed", error);
                }
                log.warn("Media {} attempt {}/{} failed; retrying in {} ms",
                        operationName, attempt, retryMaxAttempts, backoffMs);
                sleep(backoffMs);
                backoffMs = Math.min(Math.max(backoffMs * 2, backoffMs), retryMaxBackoffMs);
            }
        }
        throw new IllegalStateException("Media " + operationName + " failed");
    }

    private boolean isRetryable(Exception error) {
        if (!(error instanceof StatusRuntimeException statusError)) return false;
        return statusError.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Media retry interrupted", interrupted);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private MediaResponsiveImageResult mapResponsiveImage(Media.ResponsiveImageResponse response) {
        if (response == null) {
            return null;
        }

        return MediaResponsiveImageResult.builder()
                .originalUrl(blankToNull(response.getOriginalUrl()))
                .variants(response.getVariantsList().stream()
                        .map(variant -> ResponsiveImageVariant.builder()
                                .size(variant.getSize())
                                .url(blankToNull(variant.getUrl()))
                                .build())
                        .toList())
                .build();
    }
}

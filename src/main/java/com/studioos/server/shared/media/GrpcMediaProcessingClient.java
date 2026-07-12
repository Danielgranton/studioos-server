package com.studioos.server.shared.media;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.studioos.server.shared.enums.MediaJobStatus;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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

    public GrpcMediaProcessingClient(
            @Value("${media.service.host:localhost}") String host,
            @Value("${media.service.port:50051}") int port) {
        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = MediaServiceGrpc.newBlockingStub(channel);
        log.info("Configured StudioOS Media gRPC client for {}:{}", host, port);
    }

    @Override
    public String submitJob(String assetReference, String operation, String parametersJson) {
        Media.MediaJobRequest request = Media.MediaJobRequest.newBuilder()
                .setAssetReference(assetReference)
                .setOperation(operation)
                .setParametersJson(parametersJson == null || parametersJson.isBlank() ? "{}" : parametersJson)
                .build();

        Media.MediaJobResponse response = blockingStub.submitMediaJob(request);
        log.info("Submitted media job {} op={} asset={} status={}",
                response.getJobId(), response.getOperation(), response.getAssetReference(), response.getStatus());
        return response.getJobId();
    }

    @Override
    public MediaJobResult getJobStatus(String externalJobId) {
        Media.MediaJobLookupRequest request = Media.MediaJobLookupRequest.newBuilder()
                .setJobId(externalJobId)
                .build();

        Media.MediaJobResponse response = blockingStub.getMediaJob(request);
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

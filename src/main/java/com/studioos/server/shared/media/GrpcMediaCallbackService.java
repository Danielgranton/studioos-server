package com.studioos.server.shared.media;

import org.springframework.stereotype.Service;

import com.studioos.server.shared.enums.MediaJobStatus;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import media.MediaCallbackServiceGrpc;
import media.Media;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrpcMediaCallbackService extends MediaCallbackServiceGrpc.MediaCallbackServiceImplBase {

    private final MediaJobOrchestratorService orchestratorService;

    @Override
    public void reportMediaJob(
            Media.MediaJobCallbackRequest request,
            StreamObserver<Media.MediaJobCallbackResponse> responseObserver) {

        try {
            orchestratorService.handleCallback(toDomainRequest(request));

            responseObserver.onNext(Media.MediaJobCallbackResponse.newBuilder()
                    .setAccepted(true)
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Callback processing failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Callback processing failed")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    private MediaProcessingCallbackRequest toDomainRequest(Media.MediaJobCallbackRequest request) {
        MediaProcessingCallbackRequest callback = new MediaProcessingCallbackRequest();
        callback.setJobId(request.getJobId());
        callback.setExternalJobId(request.getExternalJobId());
        callback.setStatus(parseStatus(request.getStatus()));
        callback.setResultReference(blankToNull(request.getResultReference()));
        callback.setErrorMessage(blankToNull(request.getErrorMessage()));
        return callback;
    }

    private MediaJobStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return MediaJobStatus.QUEUED;
        }

        return switch (status.trim().toUpperCase()) {
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

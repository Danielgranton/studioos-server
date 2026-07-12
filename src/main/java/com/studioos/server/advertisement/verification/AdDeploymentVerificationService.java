package com.studioos.server.advertisement.verification;

import org.springframework.stereotype.Service;

import com.studioos.server.payment.MpesaService;
import com.studioos.server.shared.media.MediaJobResult;
import com.studioos.server.shared.media.MediaProcessingClient;
import com.studioos.server.shared.storage.PresignedUrlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdDeploymentVerificationService {

    private final MediaProcessingClient mediaProcessingClient;
    private final PresignedUrlService presignedUrlService;
    private final MpesaService mpesaService;

    public MediaProbeResponse probeMedia(MediaProbeRequest request) {
        boolean healthy = mediaProcessingClient.health();
        if (!healthy) {
            return MediaProbeResponse.builder()
                    .healthy(false)
                    .build();
        }

        String jobId = mediaProcessingClient.submitJob(
                request.getAssetReference(),
                request.getOperation(),
                request.getParametersJson());

        MediaProbeResponse.MediaProbeResponseBuilder builder = MediaProbeResponse.builder()
                .healthy(true)
                .submittedJobId(jobId);

        if (request.isPollImmediately()) {
            MediaJobResult result = mediaProcessingClient.getJobStatus(jobId);
            builder.polled(true)
                    .polledStatus(result.getStatus())
                    .resultReference(result.getResultReference())
                    .errorMessage(result.getErrorMessage());
        }

        return builder.build();
    }

    public StorageProbeResponse probeStorage(StorageProbeRequest request) {
        var metadata = presignedUrlService.objectMetadata(request.getBucket(), request.getObjectKey());
        return StorageProbeResponse.builder()
                .exists(metadata.isPresent())
                .contentLength(metadata.map(m -> m.contentLength()).orElse(null))
                .contentType(metadata.map(m -> m.contentType()).orElse(null))
                .build();
    }

    public PaymentSmokeResponse smokePayment(PaymentSmokeRequest request) {
        var result = mpesaService.initiateStkPush(
                request.getPhoneNumber(),
                request.getAmount(),
                "ad-smoke-" + System.currentTimeMillis());

        return PaymentSmokeResponse.builder()
                .accepted(result.isAccepted())
                .responseDescription(result.getResponseDescription())
                .merchantRequestId(result.getMerchantRequestId())
                .checkoutRequestId(result.getCheckoutRequestId())
                .build();
    }
}

package com.studioos.server.shared.media;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.studioos.server.shared.enums.MediaJobStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Profile("!grpc-enabled")
public class StubMediaProcessingClient implements MediaProcessingClient {

    @Override
    public boolean health() {
        return true;
    }

    @Override
    public String submitJob(String assetReference, String operation, String parametersJson) {
        String fakeJobId = UUID.randomUUID().toString();
        log.info("[STUB] Submitted job {} op={} asset={} params={}",
                fakeJobId, operation, assetReference, parametersJson);
        return fakeJobId;
    }

    @Override
    public MediaJobResult getJobStatus(String externalJobId) {
        // Stub always reports immediate success so the orchestration flow can be tested end-to-end.
        return MediaJobResult.builder()
                .jobId(externalJobId)
                .status(MediaJobStatus.SUCCESS)
                .resultReference("https://stub-media.local/result/" + externalJobId)
                .errorMessage(null)
                .build();
    }
}

package com.studioos.server.shared.media;

import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Override
    public MediaResponsiveImageResult processResponsiveImage(String assetReference, String objectKeyPrefix, int quality) {
        String base = "https://stub-media.local/responsive/" + UUID.randomUUID();
        return MediaResponsiveImageResult.builder()
                .originalUrl(base + "/original.webp")
                .variants(java.util.List.of(
                        ResponsiveImageVariant.builder().size(1024).url(base + "/1024.webp").build(),
                        ResponsiveImageVariant.builder().size(512).url(base + "/512.webp").build(),
                        ResponsiveImageVariant.builder().size(256).url(base + "/256.webp").build(),
                        ResponsiveImageVariant.builder().size(128).url(base + "/128.webp").build(),
                        ResponsiveImageVariant.builder().size(64).url(base + "/64.webp").build()))
                .build();
    }

    @Override
    public String uploadMedia(InputStream content, long contentLength, String filename, String contentType, String ownerId) {
        try {
            Path file = Files.createTempFile("studioos-profile-", ".upload");
            Files.copy(content, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Could not store uploaded image", e);
        }
    }
}

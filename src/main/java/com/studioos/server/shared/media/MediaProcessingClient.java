package com.studioos.server.shared.media;

import java.io.InputStream;

public interface MediaProcessingClient {

    /**
     * Verifies the remote Media Service is reachable and healthy.
     */
    boolean health();

    /**
     * Submits a single media operation as an async job.
     * @return the external jobId assigned by the Media Service
     */
    String submitJob(String assetReference, String operation, String parametersJson);

    /**
     * Polls the current status of a previously submitted job.
     */
    MediaJobResult getJobStatus(String externalJobId);

    /**
     * Processes a responsive image and returns the generated URLs.
     */
    MediaResponsiveImageResult processResponsiveImage(String assetReference, String objectKeyPrefix, int quality);

    /** Streams an uploaded asset into the media service and returns its reference. */
    String uploadMedia(InputStream content, long contentLength, String filename,
            String contentType, String ownerId);
}

package com.studioos.server.shared.media;

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
}

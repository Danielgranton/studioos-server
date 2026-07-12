package com.studioos.server.shared.media;

import com.studioos.server.shared.enums.MediaJobStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MediaJobResult {
    private String jobId;
    private MediaJobStatus status;
    private String assetReference;
    private String operation;
    private String parametersJson;
    private String resultReference;
    private String errorMessage;
    private long createdAtUnixMs;
    private long updatedAtUnixMs;
}

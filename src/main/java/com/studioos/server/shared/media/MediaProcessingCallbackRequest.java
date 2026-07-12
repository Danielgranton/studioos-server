package com.studioos.server.shared.media;

import com.studioos.server.shared.enums.MediaJobStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaProcessingCallbackRequest {
    private String jobId;
    private String externalJobId;
    private MediaJobStatus status;
    private String resultReference;
    private String errorMessage;

    public String resolvedJobId() {
        return jobId != null && !jobId.isBlank() ? jobId : externalJobId;
    }
}

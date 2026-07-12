package com.studioos.server.advertisement.verification;

import com.studioos.server.shared.enums.MediaJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MediaProbeResponse {
    private boolean healthy;
    private String submittedJobId;
    private boolean polled;
    private MediaJobStatus polledStatus;
    private String resultReference;
    private String errorMessage;
}

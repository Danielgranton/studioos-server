package com.studioos.server.advertisement.verification;

import lombok.Data;

@Data
public class MediaProbeRequest {
    private String assetReference;
    private String operation;
    private String parametersJson;
    private boolean pollImmediately = true;
}

package com.studioos.server.shared.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatProcessingRequest {
    private String beatId;
    private String bucket;
    private String audioKey;
    private String coverKey;
}
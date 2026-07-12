package com.studioos.server.advertisement.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class StorageProbeResponse {
    private boolean exists;
    private Long contentLength;
    private String contentType;
}

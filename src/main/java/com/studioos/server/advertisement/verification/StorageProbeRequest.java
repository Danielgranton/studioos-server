package com.studioos.server.advertisement.verification;

import lombok.Data;

@Data
public class StorageProbeRequest {
    private String bucket;
    private String objectKey;
}

package com.studioos.server.shared.storage;

import java.time.Instant;

public record StorageObjectMetadata(
        Long contentLength,
        String contentType,
        String eTag,
        Instant lastModified) {
}

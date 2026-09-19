package com.studioos.server.beatmarketplace.dto;

import java.time.LocalDateTime;

import com.studioos.server.shared.enums.MediaJobOperation;
import com.studioos.server.shared.enums.MediaJobStatus;

import lombok.Builder;

@Builder
public record BeatProcessingStatusResponse(
        MediaJobOperation operation,
        MediaJobStatus status,
        String errorMessage,
        LocalDateTime updatedAt) {
}

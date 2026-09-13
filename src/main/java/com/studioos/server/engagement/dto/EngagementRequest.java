package com.studioos.server.engagement.dto;

import com.studioos.server.engagement.EngagementTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EngagementRequest {
    @NotNull
    private EngagementTargetType targetType;

    @NotBlank
    private String targetId;
}

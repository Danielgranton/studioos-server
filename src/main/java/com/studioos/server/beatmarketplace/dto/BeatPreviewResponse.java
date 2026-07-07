package com.studioos.server.beatmarketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatPreviewResponse {
    private String beatId;
    private String previewUrl;
    private int expiresInSeconds;
}
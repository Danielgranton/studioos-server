package com.studioos.server.beatmarketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatDownloadResponse {
    private String beatId;
    private String downloadUrl;
    private int expiresInSeconds;
}
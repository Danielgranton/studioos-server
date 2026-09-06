package com.studioos.server.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioVideoUploadResponse {
    private String mediaId;
    private String uploadUrl;
    private String expiresAt;
}

package com.studioos.server.beatmarketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RefreshUploadSessionResponse {
    private String beatId;
    private String beatUploadUrl;
    private String coverUploadUrl;
    private String beatUploadSessionId;
    private String coverUploadSessionId;
}
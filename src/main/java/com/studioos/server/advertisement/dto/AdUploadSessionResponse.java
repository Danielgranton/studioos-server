package com.studioos.server.advertisement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AdUploadSessionResponse {
    private String advertisementId;
    private String uploadUrl;
    private String uploadSessionId;
    private Double estimatedFinalCpm;
    private Long estimatedImpressions;
}
package com.studioos.server.advertisement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AdUploadCompleteResponse {
    private String advertisementId;
    private String status;
}
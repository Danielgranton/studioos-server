package com.studioos.server.advertisement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AdDeliveryResponse {
    private String advertisementId;
    private String type;
    private String headline;
    private String ctaText;
    private String ctaUrl;
    private String mediaUrl;      // signed
    private String thumbnailUrl;  // signed, nullable
}
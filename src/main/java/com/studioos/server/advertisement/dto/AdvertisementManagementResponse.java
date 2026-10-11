package com.studioos.server.advertisement.dto;

import java.time.LocalDateTime;

import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdCreativeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AdvertisementManagementResponse {
    private String advertisementId;
    private String campaignId;
    private AdCreativeType type;
    private String headline;
    private String description;
    private String ctaText;
    private String ctaUrl;
    private String mediaUrl;
    private String thumbnailUrl;
    private Integer duration;
    private AdCreativeStatus status;
    private LocalDateTime createdAt;
}

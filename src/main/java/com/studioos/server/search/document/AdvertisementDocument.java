package com.studioos.server.search.document;

import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.AdCreativeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementDocument {
    private String id;
    private String campaignId;
    private Integer advertiserId;
    private String campaignTitle;
    private AdCreativeType type;
    private String headline;
    private String description;
    private String ctaText;
    private String ctaUrl;
    private String mediaUrl;
    private String thumbnailUrl;
    private Integer duration;
    private AdCreativeStatus status;
    private String createdAt;
}

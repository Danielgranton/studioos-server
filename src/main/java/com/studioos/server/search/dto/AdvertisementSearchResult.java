package com.studioos.server.search.dto;

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
public class AdvertisementSearchResult {
    private String id;
    private String campaignId;
    private String headline;
    private String description;
    private AdCreativeType type;
    private AdCreativeStatus status;
    private Double score;
}

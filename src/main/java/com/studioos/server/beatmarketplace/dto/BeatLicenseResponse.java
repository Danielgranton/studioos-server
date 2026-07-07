package com.studioos.server.beatmarketplace.dto;

import com.studioos.server.shared.enums.LicenseType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatLicenseResponse {
    private String id;
    private String beatId;
    private LicenseType type;
    private Integer price;
    private boolean commercialUse;
    private Integer maxStreams;
    private boolean allowMusicVideo;
    private boolean allowRadio;
    private boolean allowTV;
    private boolean allowModification;
    private boolean exclusive;
    private boolean active;
}
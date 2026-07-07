package com.studioos.server.beatmarketplace.dto;

import com.studioos.server.shared.enums.LicenseType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLicenseRequest {

    @NotNull
    private LicenseType type;

    @NotNull
    @Min(1)
    private Integer price;

    private boolean commercialUse;
    private Integer maxStreams;       // null = unlimited
    private boolean allowMusicVideo;
    private boolean allowRadio;
    private boolean allowTV;
    private boolean allowModification;
}
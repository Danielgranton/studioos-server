package com.studioos.server.advertisement.dto;

import com.studioos.server.shared.enums.AdCreativeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAdvertisementRequest {
    @NotNull
    private AdCreativeType type;

    @NotBlank
    private String headline;
    private String description;
    private String ctaText;
    private String ctaUrl;
    private String contentType; // e.g. "video/mp4", "image/jpeg" — for the presigned URL
}
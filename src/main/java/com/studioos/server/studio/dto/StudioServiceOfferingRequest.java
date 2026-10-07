package com.studioos.server.studio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudioServiceOfferingRequest {
    @NotBlank @Size(max = 120)
    private String name;
    private String catalogServiceId;
    private boolean active = true;
    private boolean includedInProductionPackage;
    @Min(0)
    private Integer price;
}

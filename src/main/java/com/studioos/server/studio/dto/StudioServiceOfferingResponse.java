package com.studioos.server.studio.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudioServiceOfferingResponse {
    private String id;
    private String name;
    private String catalogServiceId;
    private boolean active;
    private boolean includedInProductionPackage;
    private Integer price;
}

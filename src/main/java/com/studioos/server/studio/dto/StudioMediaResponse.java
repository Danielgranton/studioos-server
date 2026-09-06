package com.studioos.server.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioMediaResponse {
    private String id;
    private String type;
    private String url;
    private String largeUrl;
    private String mediumUrl;
    private String thumbnailUrl;
    private Integer displayOrder;
}

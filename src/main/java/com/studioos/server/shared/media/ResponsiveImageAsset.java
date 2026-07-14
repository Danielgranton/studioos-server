package com.studioos.server.shared.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsiveImageAsset {
    private String originalUrl;
    private String largeUrl;
    private String mediumUrl;
    private String thumbnailUrl;
}

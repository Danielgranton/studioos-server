package com.studioos.server.shared.media;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponsiveImageResult {
    private String originalUrl;
    private List<ResponsiveImageVariant> variants;
}

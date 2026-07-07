package com.studioos.server.beatmarketplace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatSearchCriteria {
    private String genreId;
    private String mood;
    private Integer bpmMin;
    private Integer bpmMax;
    private String keySignature;
    private String producerId;
    private String studioId;
    private Integer priceMin;
    private Integer priceMax;
}
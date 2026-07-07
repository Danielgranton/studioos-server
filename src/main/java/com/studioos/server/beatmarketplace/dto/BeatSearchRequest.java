package com.studioos.server.beatmarketplace.dto;

import lombok.Data;

@Data
public class BeatSearchRequest {
    private String genreId;
    private String mood;
    private Integer bpmMin;
    private Integer bpmMax;
    private String keySignature;
    private String producerId;
    private String studioId;
    private Integer priceMin;
    private Integer priceMax;
    private String sortBy;      // NEWEST, POPULAR, TRENDING — defaults to NEWEST
    private int page = 0;
    private int size = 20;
}
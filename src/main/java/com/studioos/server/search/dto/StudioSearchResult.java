package com.studioos.server.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class StudioSearchResult {
    private String id;
    private String studioName;
    private String location;
    private Integer pricing;
    private Double averageRating;
    private Double score;
}
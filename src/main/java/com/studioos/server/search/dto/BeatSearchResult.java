package com.studioos.server.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatSearchResult {
    private String id;
    private String title;
    private String genre;
    private Integer bpm;
    private Integer price;
    private Integer playCount;
    private Integer likeCount;
    private Double score;
    private Double rankScore;
    
    
}
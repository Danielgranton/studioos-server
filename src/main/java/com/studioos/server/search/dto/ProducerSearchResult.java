package com.studioos.server.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducerSearchResult {
    private Integer id;
    private String name;
    private String location;
    private String genre;
    private String bio;
    private String profileImage;
    private Double averageRating;
    private Integer reviewCount;
    private Double score;
}

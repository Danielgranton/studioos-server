package com.studioos.server.search.dto;

import com.studioos.server.shared.enums.SearchEntityType;
import lombok.Data;

@Data
public class FilterRequest {
    private SearchEntityType entityType;
    private String genre;
    private String location;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer minRating;
    private Integer maxRating;
    private Integer page = 0;
    private Integer size = 20;
}

package com.studioos.server.search.dto;

import com.studioos.server.shared.enums.SearchEntityType;
import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private SearchEntityType entityType;
    private String location;
    private String genre;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer page = 0;
    private Integer size = 20;
}

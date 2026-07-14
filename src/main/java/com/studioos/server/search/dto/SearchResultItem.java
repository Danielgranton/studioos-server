package com.studioos.server.search.dto;

import com.studioos.server.shared.enums.SearchEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {
    private SearchEntityType entityType;
    private String id;
    private String title;
    private String subtitle;
    private Double score;
}

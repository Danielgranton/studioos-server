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
public class TrendingResponse {
    private SearchEntityType entityType;
    private String query;
    private Long count;
}

package com.studioos.server.search.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPageResponse<T> {
    private List<T> results;
    private int page;
    private int size;
    private long total;
}

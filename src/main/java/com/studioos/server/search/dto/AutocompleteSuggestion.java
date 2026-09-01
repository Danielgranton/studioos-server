package com.studioos.server.search.dto;

import com.studioos.server.shared.enums.SearchEntityType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AutocompleteSuggestion {
    private String id;
    private String value;
    private SearchEntityType entityType;
}

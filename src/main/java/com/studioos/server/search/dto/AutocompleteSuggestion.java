package com.studioos.server.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AutocompleteSuggestion {
    private String id;
    private String text;
}
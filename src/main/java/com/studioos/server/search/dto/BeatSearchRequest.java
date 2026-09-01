package com.studioos.server.search.dto;

import lombok.Data;

@Data
public class BeatSearchRequest {
    private String q;
    private String genre;
    private Integer bpmMin;
    private Integer bpmMax;
    private int page = 0;
    private int size = 20;

    public String getQuery() {
        return q;
    }

    public void setQuery(String query) {
        this.q = query;
    }
}

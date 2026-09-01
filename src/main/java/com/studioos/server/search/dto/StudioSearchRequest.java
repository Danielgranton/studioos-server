package com.studioos.server.search.dto;

import lombok.Data;

@Data
public class StudioSearchRequest {
    private String q;
    private String location;
    private int page = 0;
    private int size = 20;

    public String getQuery() {
        return q;
    }

    public void setQuery(String query) {
        this.q = query;
    }
}

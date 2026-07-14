package com.studioos.server.search.dto;

import lombok.Data;

@Data
public class StudioSearchRequest {
    private String query;
    private String location;
    private int page = 0;
    private int size = 20;
}
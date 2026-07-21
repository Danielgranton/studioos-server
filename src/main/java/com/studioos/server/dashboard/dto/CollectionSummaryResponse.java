package com.studioos.server.dashboard.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CollectionSummaryResponse {
    private String id;
    private String name;
    private int beatCount;
    private LocalDateTime createdAt;
}
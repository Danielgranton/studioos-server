package com.studioos.server.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatPerformanceResponse {
    private String beatId;
    private String title;
    private String status;
    private String coverUrl;
    private String thumbnailUrl;
    private Integer playCount;
    private Integer downloadCount;
    private Integer likeCount;
    private long salesCount;
    private int revenue;
    private Double averageRating;   // null if no reviews yet
}

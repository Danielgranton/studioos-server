package com.studioos.server.dashboard.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ReviewGivenResponse {
    private String beatId;
    private String beatTitle;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
package com.studioos.server.beatmarketplace.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatReviewResponse {
    private String id;
    private String beatId;
    private Integer userId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
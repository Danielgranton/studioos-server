package com.studioos.server.reviews.dto;

import com.studioos.server.reviews.ReviewModerationStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewModerationRequest {
    @NotNull
    private ReviewModerationStatus status;

    @Size(max = 1000)
    private String resolution;
}

package com.studioos.server.reviews.dto;

import com.studioos.server.reviews.ReviewReactionType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewReactionRequest {
    @NotNull
    private ReviewReactionType reaction;
}

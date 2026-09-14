package com.studioos.server.reviews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewCommentRequest {
    @NotBlank
    @Size(max = 1000)
    private String body;
}

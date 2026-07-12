package com.studioos.server.beatmarketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReviewRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;

    @NotBlank(message = "Purchase ID is required")
    private String purchaseId;
}

package com.studioos.server.verification.dto;

import com.studioos.server.shared.enums.VerificationStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerificationReviewRequest {
    @NotNull
    private VerificationStatus status;

    @Size(max = 500)
    private String reason;
}

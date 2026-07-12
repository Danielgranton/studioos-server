package com.studioos.server.advertisement.verification;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentSmokeRequest {
    @NotBlank
    private String phoneNumber;

    @Min(1)
    private int amount;

    private String description;
}

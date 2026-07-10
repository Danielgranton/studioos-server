package com.studioos.server.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitiatePaymentRequest {
    @NotBlank
    private String phoneNumber;
}
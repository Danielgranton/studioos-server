package com.studioos.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneChangeRequest {
    @NotBlank(message = "New phone number is required")
    private String newPhone;
}

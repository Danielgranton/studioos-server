package com.studioos.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    // email or phone number
    @NotBlank(message = "Email or phone number is required")
    private String identifier;
}
package com.studioos.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailChangeRequest {
    @NotBlank(message = "New email is required")
    @Email(message = "Enter a valid email address")
    private String newEmail;
}

package com.studioos.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountRequest {
    @NotBlank(message = "Deletion confirmation is required")
    private String confirmation;

    private String currentPassword;
}

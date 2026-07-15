package com.studioos.server.auth.dto;

import com.studioos.server.shared.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Invalid phone number")
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    // Optional password for users who want password-based login later
    private String password;

    // Optional profile picture URL
    private String profileImage;
}

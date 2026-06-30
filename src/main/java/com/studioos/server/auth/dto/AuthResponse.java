package com.studioos.server.auth.dto;

import com.studioos.server.shared.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Integer userId;
    private String name;
    private String email;
    private String phone;
    private Role role;
}
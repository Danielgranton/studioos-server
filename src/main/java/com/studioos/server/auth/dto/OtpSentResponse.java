package com.studioos.server.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSentResponse {
    private String message;
    private String maskedEmail;
    private String maskedPhone;
}
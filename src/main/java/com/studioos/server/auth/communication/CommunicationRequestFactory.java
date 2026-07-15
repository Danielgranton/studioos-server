package com.studioos.server.auth.communication;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class CommunicationRequestFactory {

    public CommunicationRequest otp(String email, String phone, String code) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("code", code);
        return new CommunicationRequest(
                CommunicationType.OTP,
                email,
                phone,
                "StudioOS verification code",
                "Your StudioOS verification code is " + code,
                metadata);
    }

    public CommunicationRequest passwordReset(String email, String phone, String token) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("token", token);
        return new CommunicationRequest(
                CommunicationType.PASSWORD_RESET,
                email,
                phone,
                "StudioOS password reset",
                "Use this token to reset your StudioOS password: " + token,
                metadata);
    }

    public CommunicationRequest emailVerification(String email, String token) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("token", token);
        return new CommunicationRequest(
                CommunicationType.EMAIL_VERIFICATION,
                email,
                null,
                "StudioOS email verification",
                "Verify your StudioOS account using this token: " + token,
                metadata);
    }
}

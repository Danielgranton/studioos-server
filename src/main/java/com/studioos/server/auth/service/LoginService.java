package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.communication.CommunicationClient;
import com.studioos.server.auth.communication.CommunicationRequestFactory;
import com.studioos.server.auth.dto.LoginRequest;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserLookupService userLookupService;
    private final OtpService otpService;
    private final CommunicationClient communicationClient;
    private final CommunicationRequestFactory communicationRequestFactory;

    @Transactional
    public OtpSentResponse login(LoginRequest request) {
        User user = userLookupService.findByIdentifier(request.getIdentifier());
        String otp = otpService.generateAndSave(user.getEmail());
        communicationClient.send(communicationRequestFactory.otp(user.getEmail(), user.getPhone(), otp));
        log.info("Login OTP queued for user: {}", user.getEmail());

        return OtpSentResponse.builder()
                .message("Verification code sent to your email and phone")
                .maskedEmail(maskEmail(user.getEmail()))
                .maskedPhone(maskPhone(user.getPhone()))
                .build();
    }

    public OtpSentResponse resendOtp(LoginRequest request) {
        return login(request);
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1];
    }

    private String maskPhone(String phone) {
        if (phone == null) return null;
        return phone.substring(0, Math.min(4, phone.length())) + "****";
    }
}

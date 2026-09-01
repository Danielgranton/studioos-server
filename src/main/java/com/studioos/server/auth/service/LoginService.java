package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.communication.CommunicationClient;
import com.studioos.server.communication.CommunicationRequestFactory;
import com.studioos.server.auth.dto.LoginRequest;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.shared.exceptions.StudioosException;
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
        User user;
        try {
            user = userLookupService.findByIdentifier(request.getIdentifier());
        } catch (StudioosException exception) {
            if (exception.getStatus().is4xxClientError()) {
                return genericResponse();
            }
            throw exception;
        }

        String otp = otpService.generateAndSave(user.getEmail());
        communicationClient.send(communicationRequestFactory.otp(user.getEmail(), user.getPhone(), otp));
        log.info("Login OTP queued for user: {}", user.getEmail());

        // Keep the response shape identical for existing and unknown identifiers.
        return genericResponse();
    }

    public OtpSentResponse resendOtp(LoginRequest request) {
        return login(request);
    }

    private OtpSentResponse genericResponse() {
        return OtpSentResponse.builder()
                .message("If an account exists, a verification code has been sent")
                .build();
    }

}

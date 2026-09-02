package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.communication.CommunicationClient;
import com.studioos.server.communication.CommunicationRequestFactory;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final OtpService otpService;
    private final CommunicationClient communicationClient;
    private final CommunicationRequestFactory communicationRequestFactory;
    private final UserRepository userRepository;

    @Transactional
    public OtpSentResponse request(User user) {
        if (user == null) throw StudioosException.unauthorized("Authentication required");
        if (user.isEmailVerified()) {
            throw StudioosException.badRequest("Email is already verified");
        }

        String code = otpService.generateAndSave(user.getEmail());
        communicationClient.send(communicationRequestFactory.otp(user.getEmail(), null, code));
        return OtpSentResponse.builder()
                .message("Email verification code sent")
                .maskedEmail(maskEmail(user.getEmail()))
                .build();
    }

    @Transactional
    public void verify(User user, VerifyOtpRequest request) {
        if (user == null) throw StudioosException.unauthorized("Authentication required");
        if (!user.getEmail().equalsIgnoreCase(request.getIdentifier())) {
            throw StudioosException.badRequest("Verification identifier does not match your account");
        }
        otpService.verify(user.getEmail(), request.getCode());
        user.setEmailVerified(true);
        user.setAccountVerified(true);
        userRepository.save(user);
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1];
    }
}

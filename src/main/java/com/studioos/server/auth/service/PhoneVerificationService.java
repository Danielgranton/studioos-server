package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.communication.CommunicationClient;
import com.studioos.server.communication.CommunicationRequestFactory;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final OtpService otpService;
    private final CommunicationClient communicationClient;
    private final CommunicationRequestFactory communicationRequestFactory;
    private final UserRepository userRepository;

    @Transactional
    public OtpSentResponse request(User user) {
        if (user == null) throw StudioosException.unauthorized("Authentication required");
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw StudioosException.badRequest("No phone number is attached to this account");
        }
        if (user.isPhoneVerified()) {
            throw StudioosException.badRequest("Phone number is already verified");
        }

        String code = otpService.generateAndSave(user.getPhone());
        communicationClient.send(communicationRequestFactory.otp(null, user.getPhone(), code));
        return OtpSentResponse.builder()
                .message("Phone verification code sent")
                .maskedPhone(maskPhone(user.getPhone()))
                .build();
    }

    @Transactional
    public void verify(User user, VerifyOtpRequest request) {
        if (user == null) throw StudioosException.unauthorized("Authentication required");
        if (user.getPhone() == null || !user.getPhone().equals(request.getIdentifier())) {
            throw StudioosException.badRequest("Verification identifier does not match your account");
        }

        otpService.verify(user.getPhone(), request.getCode());
        user.setPhoneVerified(true);
        user.setAccountVerified(user.isEmailVerified());
        userRepository.save(user);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, Math.min(4, phone.length())) + "****";
    }
}
